package fr.ct402.dbcfs.refactor.manager

import fr.ct402.dbcfs.refactor.commons.AbstractComponent
import fr.ct402.dbcfs.Notifier
import fr.ct402.dbcfs.error
import fr.ct402.dbcfs.refactor.commons.factorioExecutableRelativeLocation
import fr.ct402.dbcfs.refactor.commons.profileRelativeModDirectory
import fr.ct402.dbcfs.persist.DbLoader
import fr.ct402.dbcfs.persist.model.Profile
import fr.ct402.dbcfs.running
import fr.ct402.dbcfs.success
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Component
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.IllegalStateException

@Component
class ProcessManager(
        dbLoader: DbLoader // FIXME remove this if still unused after rework
) : AbstractComponent() {
    private val mutex = Mutex()
    private var currentProcess: Process? = null
    final var currentProcessProfileName: String? = null; private set

    suspend fun start(profile: Profile, notifier: Notifier, save: String? = null) {
        notifier.running("Starting server...").queue()
        val factorioPath = profile.gameVersion.localPath
                ?: throw IllegalStateException("Cannot start server, game has not been downloaded")
        val saveName = save ?: "map.zip"
        val cmd = arrayOf(
                "$factorioPath/$factorioExecutableRelativeLocation",
                "--start-server", "${profile.localPath}/$saveName",
                "--server-settings", profile.findConfig("server-settings"),
                "--console-log", "${profile.localPath}/server-logs",
                "--mod-directory", "${profile.localPath}/$profileRelativeModDirectory",
        )

        mutex.withLock {
            if (currentProcess?.isAlive == true) throw IllegalStateException("Server already started")

            val process = Runtime.getRuntime().exec(cmd)
            val success = !process.waitFor(3L, TimeUnit.SECONDS)
            val stdOutput = process.inputStream.bufferedReader().use { it.readText() }
            val errorOutput = process.errorStream.bufferedReader().use { it.readText() }
            logger.warn(stdOutput)
            logger.error(errorOutput)

            if (success) {
                currentProcess = process
                currentProcessProfileName = profile.name
                notifier.success("Server successfully started").queue()
            } else {
                notifier.error("Failed to start server, see logs for details\n```\n...${stdOutput.takeLast(1500)}\n```\n" +
                        "```\n...${errorOutput.takeLast(1500)}\n```\n")
            }
        }
    }

    fun genMap(profile: Profile, notifier: Notifier): Boolean {
        val factorioPath = profile.gameVersion.localPath
        val mapPath = "${profile.localPath}/map.zip"
        if (File(mapPath).exists()) {
            notifier.success("Map already exists, skipped. To rebuild the map, use remove file first").queue()
            return true
        }

        if (factorioPath == null) {
            notifier.error("Cannot build map, game has not been downloaded")
            return false
        } else
            notifier.running("Building map...").queue()

        val process = Runtime.getRuntime().exec(arrayOf(
                "$factorioPath/$factorioExecutableRelativeLocation",
                "--create", mapPath,
                "--map-gen-settings", profile.findConfig("map-gen-settings"),
                "--map-settings", profile.findConfig("map-settings"),
                "--console-log", "${profile.localPath}/map-generation-logs",
                "--mod-directory", "${profile.localPath}/$profileRelativeModDirectory",
        ))

        val result = process.waitFor()
        val stdOutput = process.inputStream.bufferedReader().use { it.readText() }
        val errorOutput = process.errorStream.bufferedReader().use { it.readText() }
        logger.info(stdOutput)
        logger.error(errorOutput)

        return if (result == 0) {
            notifier.success("Map is ready, you can start the server").queue()
            true
        } else {
            notifier.error("Failed to build map, see logs for details\n```\n...${stdOutput.takeLast(1500)}\n```\n\n" +
                    "```\n...${errorOutput.takeLast(1500)}\n```\n");
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
        } else false
    }
}
