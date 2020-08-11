package fr.ct402.dbcfs.factorio
/*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.io.File
import java.net.URL
import kotlin.math.min

@Component
@Configuration
class ApiLoader(
        private val config: FactorioConfigProperties,
        private val dbLoader: DbLoader
) {
    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun load() {
        logger.info("Loading Factorio API manager")
        getGameVersionList()
    }

    data class Versions(
            val alpha: String,
            val demo: String,
            val headless: String
    )

    data class Latest(
            val experimental: Versions,
            val stable: Versions
    )

    fun compareVersionStrings(s1: String, s2: String): Int {
        val n1 = s1.split('.').map { it.toInt() }
        val n2 = s2.split('.').map { it.toInt() }

        for (i in 0 until min(n1.size, n2.size))
            if (n2[i] - n1[i] != 0)
                return n2[i] - n1[i]

        return 0;
    }

    fun getGameVersionList() {
        val mapper = jacksonObjectMapper()
        val latest = mapper.readValue<Latest>(URL("https://factorio.com/api/latest-releases"))
        val versionsLinks = ArrayList<String>()

        val res = get("https://www.factorio.com/download/archive",
                cookies = mapOf(Pair("session", config.cookie))
        )
        if (res.statusCode != 200) {
            logger.error("Failed to recover version list, error code : ${res.statusCode}")
            return
        }

        var html = res.text

        while (true) {
            html = html.substringAfter("href=\"/get-download/", "")
            if (html == "") break
            versionsLinks.add(html.takeWhile { it != '"' })
        }

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


        val seq = dbLoader.database.sequenceOf(GameVersions)
        val versions = versionsLinks.map(fun(path): GameVersion? {
            val parts = path.split('/')

            return GameVersion {
                versionNumber = parts[0]
                releaseType = releaseTypes[parts[1]] ?: return null
                platform = platforms[parts[2]] ?: return null
                isStable = compareVersionStrings(parts[0], latest.stable.alpha) >= 0
                this.path = path
            }
        })
                .filterNotNull()
                .onEach { item ->
                    val dbItem = seq.find { it.path eq item.path }
                    if (dbItem == null)
                        seq.add(item)
                } // TODO rework as a true sync process
                .groupBy { it.versionNumber }

        logger.info("Version sync is complete, ${versions.size} version(s) found.")

        //TODO Make something with versions (fill the DB)


        val toDownload = versions[latest.experimental.headless]
                ?.filter { it.platform == Platform.LINUX64 }
                ?.find { it.releaseType == ReleaseType.HEADLESS }
        if (toDownload == null) {
            logger.error("Could not find ${latest.experimental.headless} headless")
            return
        }

        logger.info("Trying download of https://www.factorio.com/get-download/${toDownload.path}")
        val r = get("https://www.factorio.com/get-download/${toDownload.path}",
                cookies = mapOf(Pair("session", config.cookie)),
                stream = true
        )

        val extension = r.headers["Content-Disposition"]
                ?.substringAfter("filename=", "")
                ?.substringAfter('.', "")
                ?.split('.')
                ?.filter { it.all { it.isLetter() } && it != "" }
                ?.reduce { acc, s -> "${acc}.${s}" }
        logger.info("Inferred file extension : $extension")

        val file = File("/mnt/factorio-${toDownload.versionNumber}-headless.${extension ?: "unknown"}")
        if (!file.createNewFile()) {
            logger.error("Error, directory is not empty")
            file.delete()
            logger.error("Deleting for test purpose")
        }

        var i = 0
        for (chunk in r.contentIterator(1024)) {
            if (i % 10240 == 0)
                logger.info("Downloading... Approx ${i / 1024}MB")
            ++i
            file.appendBytes(chunk)
        }

        logger.info("Successfully downloaded file to ${file.path}")

        // Trying to uncompress that stuff
        val folder = File(file.path.removeSuffix(".$extension"))
        if (!folder.exists())
            folder.mkdir()

        val cmd = "tar -xJ -C $folder -f ${file.path}"
        logger.info("Running : $cmd")

        val p = Runtime.getRuntime().exec(cmd)
        while (p.isAlive); // Lolilol attente active :D
        if (p.exitValue() == 0)
            logger.info("Successfully extracted archive")
        else {
            logger.error("Failed to extract archive to destination")
            logger.warn(p.inputStream.bufferedReader().use { it.readText() })
            logger.error(p.errorStream.bufferedReader().use { it.readText() })
        }

        val factorio = Runtime.getRuntime().exec("$folder/factorio/bin/x64/factorio --help")
        while (factorio.isAlive);
        logger.info("Successfully ran factorio !!!")
        logger.info(factorio.inputStream.bufferedReader().use { it.readText() })
    }
}
*/