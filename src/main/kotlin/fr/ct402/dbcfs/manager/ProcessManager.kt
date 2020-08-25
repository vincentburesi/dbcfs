package fr.ct402.dbcfs.manager

import fr.ct402.dbcfs.commons.AbstractComponent
import fr.ct402.dbcfs.commons.factorioExecutableRelativeLocation
import fr.ct402.dbcfs.discord.Notifier
import fr.ct402.dbcfs.persist.DbLoader
import fr.ct402.dbcfs.persist.model.Profile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Component
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.IllegalStateException

@Component
class ProcessManager (
        dbLoader: DbLoader
): AbstractComponent() {
    private val mutex = Mutex()
    private var currentProcess: Process? = null
    final var currentProcessProfileName: String? = null; private set

    suspend fun start(profile: Profile, notifier: Notifier) {
        val factorioPath = profile.gameVersion.localPath ?: throw IllegalStateException("Cannot start server, game has not been downloaded")
        val cmd = arrayOf("$factorioPath/$factorioExecutableRelativeLocation",
                "--start-server", "${profile.localPath}/map.zip",
                "--server-settings", profile.findConfig("server-settings"),
                "--console-log", "${profile.localPath}/test-logfile-run" //TODO
//                "--mod-directory", "/mnt/...", //TODO
//                "--map-gen-settings", "map.zip",
//                "--map-settings", "map.zip",
//                "--config", "config file",
        ).let {
            it.plus(arrayOf("--server-whitelist", profile.serverWhitelist ?: return@let it))
        }

        mutex.withLock {
            if (currentProcess?.isAlive == true) throw IllegalStateException("Server already started")

            val p = Runtime.getRuntime().exec(cmd)
            val success = !p.waitFor(3L, TimeUnit.SECONDS)

            if (success) {
                currentProcess = p
                currentProcessProfileName = profile.name
                notifier.success("Server successfully started")
            } else {
                notifier.error("Failed to start server, see logs for details")
                logger.warn(p.inputStream.bufferedReader().use { it.readText() })
                logger.error(p.errorStream.bufferedReader().use { it.readText() })
            }
        }
    }

    fun genMap(profile: Profile, notifier: Notifier): Boolean {
        val factorioPath = profile.gameVersion.localPath
        if (factorioPath == null) {
            notifier.error("Cannot build map, game has not been downloaded")
            return false
        } else
            notifier.update("Building map...")

        val p = Runtime.getRuntime().exec(arrayOf("$factorioPath/$factorioExecutableRelativeLocation",
                "--create", "${profile.localPath}/map.zip",
                "--map-gen-settings", profile.findConfig("map-gen-settings"),
                "--map-settings", profile.findConfig("map-settings"),
                "--console-log", "/mnt/test-logfile-genmap" // TODO
//                "--mod-directory", "/mnt/...", //TODO
//                "--config", "config file",
        ))

        return if (p.waitFor() == 0) {
            notifier.success("Map is ready, you can start the server")
            true
        } else {
            notifier.error("Failed to build map, see logs for details")
            logger.warn(p.inputStream.bufferedReader().use { it.readText() })
            logger.error(p.errorStream.bufferedReader().use { it.readText() })
            false
        }
    }

    private fun Profile.findConfig(config: String): String {
        return File("$localPath/$config.json").run {
            when {
                exists() -> absolutePath
                gameVersion.localPath != null -> "${gameVersion.localPath}/factorio/data/$config.example.json"
                else -> throw IllegalStateException("Cannot find default config when game has not been downloaded (Redesign for fix)")
            }
        }
    }

    fun stop(): Boolean {
        return if (currentProcess?.isAlive == true) {
            currentProcess?.destroy()
            true
        }
        else false
    }
}
