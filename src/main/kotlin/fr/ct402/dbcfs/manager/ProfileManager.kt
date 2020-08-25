package fr.ct402.dbcfs.manager

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fr.ct402.dbcfs.commons.*
import fr.ct402.dbcfs.discord.Notifier
import fr.ct402.dbcfs.factorio.api.DownloadApiService
import fr.ct402.dbcfs.factorio.config.MapGenSettings
import fr.ct402.dbcfs.factorio.config.MapSettings
import fr.ct402.dbcfs.factorio.config.ServerSettings
import fr.ct402.dbcfs.persist.DbLoader
import fr.ct402.dbcfs.persist.model.*
import fr.ct402.dbcfs.persist.model.GameVersion.Platform
import fr.ct402.dbcfs.persist.model.GameVersion.BuildType
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.dsl.like
import me.liuwj.ktorm.entity.*
import net.dv8tion.jda.api.entities.Message
import org.springframework.stereotype.Component
import java.io.File
import java.lang.Exception
import java.security.SecureRandom
import java.time.LocalDateTime

@Component
class ProfileManager (
        val downloadApiService: DownloadApiService,
        val processManager: ProcessManager,
        dbLoader: DbLoader
): AbstractComponent() {
    final var currentProfile: Profile? = null
        private set
    val currentProfileOrThrow
        get() = currentProfile ?: throw NoCurrentProfileException()

    private val profileSequence = dbLoader.database.sequenceOf(Profiles)
    private val gameVersionSequence = dbLoader.database.sequenceOf(GameVersions)
    private val secureRandom = SecureRandom()
    private val jsonMapper = jacksonObjectMapper()

    private fun swapProfile(profile: Profile?) {
        currentProfile = profile
    }

    fun getProfileByName(name: String): Profile? {
        return if (currentProfile?.name == name)
            currentProfile
        else
            profileSequence.find { it.name eq name }
    }

    /**
     * Attempts to swap current profile
     */
    fun swapProfile(name: String) =
            swapProfile(profileSequence.find { it.name eq name } ?: throw ProfileNotFoundException(name))

    /**
     * Attempts to create a profile with the given name and parameters
     */
    fun createProfile(
            name: String,
            targetGameVersion: String? = null,
            allowExperimental: Boolean = false,
            notifier: Notifier
    ) {
        val existing = profileSequence.find { it.name eq name }
        if (existing != null) {
            notifier.error("Profile already exists")
            return
        }

        val target = targetGameVersion ?: DownloadApiService.getLatestVersions().stable.headless
        notifier.update("Attempting to create profile with target version $target")
        val profile = Profile().apply {
            this.name = name
            this.targetGameVersion = target
            this.allowExperimental = allowExperimental
            this.gameVersion = getMatchingVersion(target) ?: throw MatchingVersionNotFound(target)
        }
        File(profile.localPath).apply { if (!exists()) mkdirs() }
        profileSequence.add(profile)
        swapProfile(profile)
        notifier.success("Profile $name successfully created with version ${profile.gameVersion.versionNumber}")
    }

    /**
     * Attempts to remove a profile with given name
     */
    fun removeProfile(name: String, notifier: Notifier) {
        if (processManager.currentProcessProfileName == name) processManager.stop()
        if (currentProfile?.name == name) swapProfile(null)
        val profile = profileSequence.find { it.name eq name }
        if (profile != null) {
            profileSequence.removeIf { it.name eq name }
            File(profile.localPath).apply { if (exists()) deleteRecursively() }
            notifier.success("Profile $name successfully removed")
        } else
            notifier.error("Error, could not find profile with given name")
    }

    private fun getMatchingVersion(approxVersion: String,
                           buildType: BuildType = BuildType.HEADLESS,
                           platform: Platform = Platform.LINUX64): GameVersion? {
        val candidates = gameVersionSequence
                .filter { it.versionNumber like "$approxVersion%" }
                .toList()
                .groupBy { it.versionNumber }
        val bestMatch = candidates.keys
                .filter { compareVersionStrings(it, approxVersion) == 0 }
                .sortedWith(Comparator { s1, s2 -> compareVersionStrings(s1, s2) })
                .firstOrNull()
        val version = candidates[bestMatch]?.first { it.buildType == buildType && it.platform == platform }
        if (version == null) logger.warn("Error could not find version for $approxVersion")
        return version
    }

    /**
     * Attempt to download the latest game version for the current profile
     */
    fun downloadGame(notifier: Notifier): Boolean {
        return downloadGame(currentProfile?.gameVersion ?: return false, notifier)
    }

    private fun downloadGame(version: GameVersion, notifier: Notifier): Boolean {
        if (version.platform != Platform.LINUX64
                || version.buildType != BuildType.HEADLESS) {
            notifier.error("Error game version does not match linux64-headless")
            return false
        }


        if (version.localPath != null) {
            notifier.success("Game version already downloaded")
            return true
        } else
            notifier.update("Starting download for ${version.versionNumber}...")

        val destination = File("$baseDataDir/bin/${version.versionNumber}")
        if (destination.exists())
            destination.deleteRecursively()
        destination.mkdirs()

        return if (downloadApiService.downloadGameClient(version.path, destination.absolutePath, notifier)) {
            version.apply { localPath = destination.absolutePath }.flushChanges()
            true
        } else
            false
    }

    /**
     * TODO Make generic (when with objecttype)
     */
    fun setServerSettings(profileName: String, serverSettings: ServerSettings): Boolean {
        val profile =
                (if (currentProfile?.name == profileName) currentProfile
                else profileSequence.find { it.name eq profileName })
                        ?: return false

        return {
            File(profile.localPath + "/server-settings.json")
                    .writeText(jacksonObjectMapper().writeValueAsString(serverSettings))
            true
        } catch { false }
    }

    fun uploadConfigFile(profile: Profile, settings: Any): Boolean {
        val configFileName = when (settings) {
            is ServerSettings -> "server-settings.json"
            is MapSettings -> "map-settings.json"
            is MapGenSettings -> "map-gen-settings.json"
            else -> return false
        }

        val jsonString = jsonMapper.writeValueAsString(settings)
        logger.info("Received json (${settings::class}): $jsonString")
        File(profile.localPath + "/" + configFileName).writeText(jsonString)
        return true
    }

    fun uploadConfigFile(attachment: Message.Attachment): Boolean {
        val profile = currentProfile ?: return false
        File(profile.localPath + "/" + attachment.fileName).apply {
            logger.info("Trying to download ${attachment.proxyUrl} to ${this.absolutePath}")
            attachment.downloadToFile(this).thenAccept { file ->
                logger.info("Saved attachment to " + file.getName())
            }.exceptionally {
                logger.error("Error could not download attachement : ${it.message}")
                throw Exception(it.message)
            }
        }
        logger.info("Success!")
        return true
    }

    fun generateAuthToken(notifier: Notifier) {
        logger.info("allowed chars $tokenAllowedChars")
        val now = LocalDateTime.now()
        val profile = currentProfile
        if (profile == null) {
            notifier.error("No profile is currently selected, please select or create a profile first (See create profile or swap)") //FIXME
            return
        }

        profile.apply {
            if (token == null || tokenExpiration?.isAfter(now) != true)
                token = (1..tokenLength).map {
                    secureRandom.nextInt(tokenAllowedChars.length)
                }.map(tokenAllowedChars::get).joinToString("")
            tokenExpiration = now.plusMinutes(tokenValidityInMinutes)
        }.flushChanges()

        notifier.success("You can edit your profile here : http://localhost:8080/edit/${profile.name}/${profile.token}\n" +
                "The link will be valid for the next $tokenValidityInMinutes minutes")
    }
}