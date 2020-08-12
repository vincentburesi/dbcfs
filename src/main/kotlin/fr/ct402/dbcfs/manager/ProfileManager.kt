package fr.ct402.dbcfs.manager

import fr.ct402.dbcfs.commons.baseDataDir
import fr.ct402.dbcfs.commons.compareVersionStrings
import fr.ct402.dbcfs.commons.getLogger
import fr.ct402.dbcfs.factorio.api.DownloadApiService
import fr.ct402.dbcfs.persist.DbLoader
import fr.ct402.dbcfs.persist.model.*
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.dsl.like
import me.liuwj.ktorm.entity.filter
import me.liuwj.ktorm.entity.find
import me.liuwj.ktorm.entity.sequenceOf
import me.liuwj.ktorm.entity.toList
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.io.File

@Component
class ProfileManager (
        val downloadApiService: DownloadApiService,
        dbLoader: DbLoader
) {
    val logger = getLogger()
    private var currentProfile: Profile? = null
    private val profileSequence = dbLoader.database.sequenceOf(Profiles)
    private val gameVersionSequence = dbLoader.database.sequenceOf(GameVersions)

    fun swapProfile(profile: Profile) {
        currentProfile = profile
    }

    fun swapProfile(name: String) : Boolean {
        swapProfile(profileSequence.find { it.name eq name } ?: return false)
        return true
    }

    fun createProfile(
            name: String,
            targetGameVersion: String = DownloadApiService.getLatestVersions().stable.headless,
            allowExperimental: Boolean = false
    ) {
        val profile = Profile().apply {
            this.name = name
            this.targetGameVersion = targetGameVersion
            this.allowExperimental = allowExperimental
            //TODO set specific game version
            flushChanges()
        }
        swapProfile(profile)
    }

    fun getMatchingVersion(approxVersion: String): List<GameVersion>? {
        val candidates = gameVersionSequence
                .filter { it.versionNumber like "$approxVersion%" }
                .toList()
                .groupBy { it.versionNumber }
        val bestMatch = candidates.keys
                .filter { compareVersionStrings(it, approxVersion) == 0 }
                .sortedWith(Comparator { s1, s2 -> compareVersionStrings(s1, s2) })
                .first()
        return candidates[bestMatch]
    }

    fun downloadGame(version: String): Boolean {
        return downloadGame(
                gameVersionSequence
                .filter { it.buildType eq BuildType.HEADLESS }
                .filter { it.platform eq Platform.LINUX64 }
                .find { it.versionNumber eq version }
                ?: return false
        )
    }

    fun downloadGame(version: GameVersion): Boolean {
        if (version.platform != Platform.LINUX64
                || version.buildType != BuildType.HEADLESS
                || version.localPath != null)
            return false
        logger.info("Starting download procedure for ${version.versionNumber}")
        val destination = File("$baseDataDir/bin/${version.versionNumber}")

        if (destination.exists())
            return false
        destination.mkdirs()

        if (!downloadApiService.downloadGameClient(version.path, destination.absolutePath))
            return false
        version.apply { localPath = destination.absolutePath }.flushChanges()
        return true
    }

    @Order(2)
    @EventListener(ApplicationReadyEvent::class)
    fun test() {
        logger.info("STARTING TEST")
        downloadApiService.syncGameVersions()
        downloadGame(getMatchingVersion("0.17")
                ?.first { it.buildType == BuildType.HEADLESS && it.platform == Platform.LINUX64 }
                ?.apply {
                    logger.info(this.toString())
                    logger.info(this.localPath ?: "THIS IS NULL AS EXPECTED")
                }
                ?: return
        ).let { if (!it) logger.error("FAILURE") else logger.info("SUCCESS") }
    }
}