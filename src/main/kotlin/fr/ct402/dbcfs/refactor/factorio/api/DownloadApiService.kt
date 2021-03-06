package fr.ct402.dbcfs.refactor.factorio.api;

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fr.ct402.dbcfs.refactor.commons.*
import fr.ct402.dbcfs.Notifier
import fr.ct402.dbcfs.commons.compareVersionStrings
import fr.ct402.dbcfs.error
import fr.ct402.dbcfs.persist.DbLoader
import fr.ct402.dbcfs.persist.model.GameVersion
import fr.ct402.dbcfs.persist.model.GameVersions
import fr.ct402.dbcfs.persist.model.GameVersion.Platform
import fr.ct402.dbcfs.persist.model.GameVersion.BuildType
import fr.ct402.dbcfs.persist.model.Profile
import fr.ct402.dbcfs.running
import fr.ct402.dbcfs.success
import khttp.get
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.entity.add
import me.liuwj.ktorm.entity.find
import me.liuwj.ktorm.entity.sequenceOf
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import java.io.File
import java.net.URL

@Service
@Configuration
class DownloadApiService(
        private val config: Config,
        dbLoader: DbLoader
): AbstractComponent() {
    companion object {
        val jsonMapper = jacksonObjectMapper()
        fun getLatestVersions() = jsonMapper.readValue<Latest>(URL("https://factorio.com/api/latest-releases"))
    }

    val gameVersionSequence = dbLoader.database.sequenceOf(GameVersions)
    val factorioDownloadUrl = "https://www.factorio.com/get-download/"

    data class Versions(val alpha: String, val demo: String, val headless: String)
    data class Latest(val experimental: Versions, val stable: Versions)
    val buildTypes = mapOf("alpha" to BuildType.ALPHA, "headless" to BuildType.HEADLESS)
    val platforms = mapOf(
            "win64" to Platform.WIN64,
            "win32" to Platform.WIN32,
            "win64-manual" to Platform.WIN64_MANUAL,
            "win32-manual" to Platform.WIN32_MANUAL,
            "linux64" to Platform.LINUX64,
            "linux32" to Platform.LINUX32,
            "osx" to Platform.OSX
    )

    private fun parseDownloadLinks(text: String, stable: String): List<GameVersion> {
        val versionLinks = ArrayList<String>()
        var html = text

        while (true) {
            html = html.substringAfter("href=\"/get-download/", "")
            if (html == "") break
            versionLinks.add(html.takeWhile { it != '"' })
        }

        logger.info(versionLinks.reduce { acc, s -> "$acc\n$s" })

        return versionLinks.mapNotNull(fun(path): GameVersion? {
            val parts = path.split('/')

            return GameVersion {
                versionNumber = parts[0]
                buildType = buildTypes[parts[1]] ?: return null
                platform = platforms[parts[2]] ?: return null
                isStable = compareVersionStrings(parts[0], stable) <= 0
                localPath = null
                this.path = path
            }
        })
    }

    private fun updateDb(versions: List<GameVersion>) {
        versions.forEach { item ->
            val dbItem = gameVersionSequence.find { it.path eq item.path }
            if (dbItem == null)
                gameVersionSequence.add(item)
            else
                dbItem.apply {
                    versionNumber = item.versionNumber
                    buildType = item.buildType
                    platform = item.platform
                    isStable = item.isStable
                }.flushChanges()
        }
    }

    fun syncGameVersions(notifier: Notifier) {
        notifier.running("Starting game version sync...").queue()
        val latest = getLatestVersions()
        val res = get("https://www.factorio.com/download/archive",
                cookies = mapOf(Pair("session", config.factorio.cookie))
        )

        if (res.statusCode != 200) {
            notifier.error("Failed to recover version list, error code : ${res.statusCode}")
            return
        }

        notifier.running("Contacted Factorio server, parsing...").queue()
        val versions = parseDownloadLinks(res.text, latest.stable.alpha)
        notifier.running("Contacted Factorio server, Updating DB...").queue()
        updateDb(versions)
        notifier.success("Successfully synced game versions").flush()
    }

    private fun getFileNameFromHeader(headers: Map<String, String>) = headers["Content-Disposition"]
            ?.substringAfter("filename=", "")

    private fun String.inferFileExtension() = this
            .substringAfter('.', "")
            .split('.')
            .filter { it.all { it.isLetter() } && it != "" }
            .reduce { acc, s -> "${acc}.${s}" }

    private fun extractArchive(path: String, extension: String, notifier: Notifier): Boolean {
        val folder = File(path)
        val archive = File("$path.$extension")

        if (!folder.exists()) //TODO Redundant ?
            folder.mkdir()

        val cmd = when (extension) {
            "tar.xz" -> arrayOf("tar", "-xJ", "-C", path, "-f", "$path.$extension")
            else -> return false
        }
        val p = Runtime.getRuntime().exec(cmd)
        p.waitFor()
        if (p.exitValue() != 0) {
            notifier.error("Extraction failed, see logs for details")
            logger.error("Command Failed : $cmd")
            logger.error("\n" + p.inputStream.bufferedReader().use { it.readText() })
            logger.error("\n" + p.errorStream.bufferedReader().use { it.readText() })
            return false
        } else
            notifier.success("Extraction successful").queue()
        archive.delete()
        return true
    }

    fun downloadToProfile(webPath: String, profile: Profile, notifier: Notifier? = null): String {
        val downloadUrl = "$factorioDownloadUrl$webPath"
        notifier?.running("Starting download from $downloadUrl...")?.queue()

        val res = get(downloadUrl,
                cookies = mapOf(Pair("session", config.factorio.cookie)),
                stream = true
        )

        if (res.statusCode != 200) {
            logger.error("downloadToProfile: Received ${res.statusCode} from $downloadUrl, details below:\n" +
                    res.content)
            throw FactorioApiErrorException("Received ${res.statusCode} from $downloadUrl")
        }

        val fileName = getFileNameFromHeader(res.headers)
                ?: throw FactorioApiErrorException("Could not read filename from headers")
        val size = res.headers["Content-Length"]?.toLong() ?: -1L

        val destination = File("${profile.localPath}/$fileName").apply {
            if (!createNewFile()) {
                notifier?.error("Error, file already exists")
                return fileName
            }
        }

        var i = 0
        for (chunk in res.contentIterator(1024)) {
            destination.appendBytes(chunk)
            if (++i % 1024 == 0)
                notifier?.running("Downloading ${fileSizeAsString(i.toLong() * 1024)}" +
                        if (size == -1L) "..." else " of ${fileSizeAsString(size)}...")?.queue()
        }

        notifier?.success("Download of **$fileName** complete")?.queue()
        return fileName
    }

    fun downloadGameClient(webPath: String, localPath: String, notifier: Notifier): Boolean {
        val res = get("$factorioDownloadUrl$webPath",
                cookies = mapOf(Pair("session", config.factorio.cookie)),
                stream = true
        )
        if (res.statusCode != 200) return false

        val extension = getFileNameFromHeader(res.headers)?.inferFileExtension() ?: return false
        logger.debug("Inferred file extension : $extension")

        val archive = File("$localPath.$extension")
        if (!archive.createNewFile()) {
            logger.error("Error, directory is not empty")
        }

        for (chunk in res.contentIterator(1024))
            archive.appendBytes(chunk)
        notifier.running("Download complete, extracting files...").queue()

        return extractArchive(localPath, extension, notifier)
    }
}
