package fr.ct402.dbcfs.manager

import fr.ct402.dbcfs.commons.factorioExecutableRelativeLocation
import fr.ct402.dbcfs.commons.getLogger
import fr.ct402.dbcfs.persist.DbLoader
import fr.ct402.dbcfs.persist.model.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class ProcessManager (
        dbLoader: DbLoader
) {
    val logger = getLogger()
    private var currentProcess: Process? = null
    final var currentProcessProfileName: String? = null; private set

    fun start(profile: Profile): Boolean {
        val factorioPath = profile.gameVersion.localPath ?: return false
        if (currentProcess?.isAlive == true) return false
        val cmd = arrayOf("$factorioPath/$factorioExecutableRelativeLocation",
                "--start-server", "/mnt/test-map.zip", //FIXME
                "--server-settings", profile.serverSettings ?: "$factorioPath/factorio/data/server-settings.example.json",
                "--console-log", "/mnt/test-logfile-run" //TODO
//                "--mod-directory", "/mnt/...", //TODO
//                "--map-gen-settings", "map.zip",
//                "--map-settings", "map.zip",
//                "--config", "config file",
        ).let { it.plus(arrayOf("--server-whitelist", profile.serverWhitelist ?: return@let it)) }
        val p = Runtime.getRuntime().exec(cmd)
        val success = !p.waitFor(3L, TimeUnit.SECONDS)
        if (success) {
            currentProcess = p
            currentProcessProfileName = profile.name
        } else {
            logger.error("Failed to build map, error log below :")
            logger.warn(p.inputStream.bufferedReader().use { it.readText() })
            logger.error(p.errorStream.bufferedReader().use { it.readText() })
        }
        return success
    }

    fun genMap(profile: Profile): Boolean {
        val factorioPath = profile.gameVersion.localPath ?: return false
        val p = Runtime.getRuntime().exec(arrayOf("$factorioPath/$factorioExecutableRelativeLocation",
                "--create", "/mnt/test-map.zip", //FIXME
                "--map-gen-settings", profile.mapGenSettings ?: "$factorioPath/factorio/data/map-gen-settings.example.json",
                "--map-settings", profile.mapSettings ?: "$factorioPath/factorio/data/map-settings.example.json",
                "--console-log", "/mnt/test-logfile-genmap" // TODO
//                "--mod-directory", "/mnt/...", //TODO
//                "--config", "config file",
        ))
        val success = p.waitFor() == 0
        if (!success) {
            logger.error("Failed to build map, error log below :")
            logger.warn(p.inputStream.bufferedReader().use { it.readText() })
            logger.error(p.errorStream.bufferedReader().use { it.readText() })
        }
        return success
    }

    fun stop(): Boolean {
        return if (currentProcess?.isAlive == true) {
            currentProcess?.destroy()
            true
        }
        else false
    }
}
