package fr.ct402.dbcfs.factorio.api;

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fr.ct402.dbcfs.factorio.FactorioConfigProperties
import fr.ct402.dbcfs.persist.DbLoader
import fr.ct402.dbcfs.persist.model.GameVersion
import fr.ct402.dbcfs.persist.model.GameVersions
import fr.ct402.dbcfs.persist.model.Platform
import fr.ct402.dbcfs.persist.model.ReleaseType
import khttp.get
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.entity.add
import me.liuwj.ktorm.entity.find
import me.liuwj.ktorm.entity.sequenceOf
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.io.File
import java.net.URL
import kotlin.math.min

@Component
@Configuration
class DownloadApiParser(val config: FactorioConfigProperties, private val dbLoader: DbLoader){
    val logger = LoggerFactory.getLogger(DownloadApiParser::class.java)
    val gameVersionSequence = dbLoader.database.sequenceOf(GameVersions)
    val jsonMapper = jacksonObjectMapper()
    val factorioDownloadUrl = "https://www.factorio.com/get-download/"

    data class Versions(val alpha: String, val demo: String, val headless: String)
    data class Latest(val experimental: Versions, val stable: Versions)
    val releaseTypes = mapOf("alpha" to ReleaseType.ALPHA, "headless" to ReleaseType.HEADLESS)
    val platforms = mapOf(
            "win64" to Platform.WIN64,
            "win32" to Platform.WIN32,
            "win64-manual" to Platform.WIN64_MANUAL,
            "win32-manual" to Platform.WIN32_MANUAL,
            "linux64" to Platform.LINUX64,
            "linux32" to Platform.LINUX32,
            "osx" to Platform.OSX
    )

    // TODO Move to helper file
    fun compareVersionStrings(s1: String, s2: String): Int {
        val n1 = s1.split('.').map { it.toInt() }
        val n2 = s2.split('.').map { it.toInt() }

        for (i in 0 until min(n1.size, n2.size))
            if (n2[i] - n1[i] != 0)
                return n2[i] - n1[i]

        return 0;
    }

    private fun parseDownloadLinks(text: String, stable: String): List<GameVersion> {
        val versionLinks = ArrayList<String>()
        var html = text

        while (true) {
            html = html.substringAfter("href=\"/get-download/", "")
            if (html == "") break
            versionLinks.add(html.takeWhile { it != '"' })
        }

        return versionLinks.mapNotNull(fun(path): GameVersion? {
            val parts = path.split('/')

            return GameVersion {
                versionNumber = parts[0]
                releaseType = releaseTypes[parts[1]] ?: return null
                platform = platforms[parts[2]] ?: return null
                isStable = compareVersionStrings(parts[0], stable) >= 0
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
                    releaseType = item.releaseType
                    platform = item.platform
                    isStable = item.isStable
                }.flushChanges()
        }
    }

    fun syncGameVersions() {
        val latest = jsonMapper.readValue<Latest>(URL("https://factorio.com/api/latest-releases"))
        val res = get("https://www.factorio.com/download/archive",
                cookies = mapOf(Pair("session", config.cookie))
        )

        if (res.statusCode != 200) {
            logger.error("Failed to recover version list, error code : ${res.statusCode}")
            return
        }

        val versions = parseDownloadLinks(res.text, latest.stable.alpha)
        updateDb(versions)
    }

    private fun inferFileExtension(headers: Map<String, String>) = headers["Content-Disposition"]
            ?.substringAfter("filename=", "")
            ?.substringAfter('.', "")
            ?.split('.')
            ?.filter { it.all { it.isLetter() } && it != "" }
            ?.reduce { acc, s -> "${acc}.${s}" }

    fun extractArchive(path: String, extension: String) {
        val folder = File(path)
        if (!folder.exists())
            folder.mkdir()
        val cmd = when (extension) {
            "tar.xz" -> "tar -xJ -C $path -f $path.$extension"
            else -> return //TODO throw proper exception
        }
        val p = Runtime.getRuntime().exec(cmd)

        while (p.isAlive)
            ; //FIXME active wait

        if (p.exitValue() != 0) {
            logger.error("Command Failed : $cmd")
            logger.error("\n" + p.inputStream.bufferedReader().use { it.readText() })
            logger.error("\n" + p.errorStream.bufferedReader().use { it.readText() })
        }

    }

    fun downloadGameClient(webPath: String, localPath: String) {
        val r = get("$factorioDownloadUrl$webPath",
                cookies = mapOf(Pair("session", config.cookie)),
                stream = true
        )
        val extension = inferFileExtension(r.headers) ?: return //TODO throw proper exception
        logger.debug("Inferred file extension : $extension")

        val archive = File("$localPath.$extension")
        if (!archive.createNewFile()) {
            logger.error("Error, directory is not empty")
        }

        for (chunk in r.contentIterator(1024))
            archive.appendBytes(chunk)

        extractArchive(localPath, extension)
    }
}
