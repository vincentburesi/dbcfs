package fr.ct402.dbcfs.refactor.manager

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fr.ct402.dbcfs.refactor.factorio.api.DownloadApiService
import fr.ct402.dbcfs.refactor.factorio.config.MapGenSettings
import fr.ct402.dbcfs.refactor.factorio.config.MapSettings
import fr.ct402.dbcfs.refactor.factorio.config.ServerSettings
import fr.ct402.dbcfs.persist.DbLoader
import fr.ct402.dbcfs.refactor.commons.Config
import fr.ct402.dbcfs.refactor.commons.*
import fr.ct402.dbcfs.refactor.discord.Notifier
import fr.ct402.dbcfs.persist.model.GameVersion
import fr.ct402.dbcfs.persist.model.GameVersion.Platform
import fr.ct402.dbcfs.persist.model.GameVersion.BuildType
import fr.ct402.dbcfs.persist.model.GameVersions
import fr.ct402.dbcfs.persist.model.Profile
import fr.ct402.dbcfs.persist.model.Profiles
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.dsl.like
import me.liuwj.ktorm.entity.*
import net.dv8tion.jda.api.entities.Message
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.io.File
import java.lang.Exception
import java.security.SecureRandom
import java.time.LocalDateTime

@Component
@Configuration
class ProfileManager (
        val config: Config,
        val downloadApiService: DownloadApiService,
        val processManager: ProcessManager,
        val dbLoader: DbLoader
): AbstractComponent() {
    final var currentProfile: Profile? = null
        private set
    val currentProfileOrThrow
        get() = currentProfile ?: throw NoCurrentProfileException()

    private fun profileSequence() = dbLoader.database.sequenceOf(Profiles)
    private fun gameReleaseSequence() = dbLoader.database.sequenceOf(GameVersions)
    private val secureRandom = SecureRandom()
    private val jsonMapper = jacksonObjectMapper()

    fun getAllProfiles(): List<Profile> = profileSequence().toList()
    fun getAllGameReleases(): List<GameVersion> = gameReleaseSequence().toList()
    fun getAllGameReleasesForProfile(profile: Profile): List<GameVersion> =
            gameReleaseSequence().filter { it.versionNumber eq profile.gameVersion.versionNumber }.toList()

    private fun swapProfile(profile: Profile?) {
        currentProfile = profile
    }

    fun getProfileByNameOrThrow(name: String) = getProfileByName(name) ?: throw ProfileNotFoundException(name)
    fun getProfileByName(name: String): Profile? {
        return if (currentProfile?.name == name)
            currentProfile
        else
            profileSequence().find { it.name eq name }
    }

    fun checkNameAvailableOrThrow(name: String) {
        if (getProfileByName(name) != null) throw ProfileNameNotAvailableException(name)
    }

    /**
     * Attempts to swap current profile
     */
    fun swapProfile(name: String) =
            swapProfile(profileSequence().find { it.name eq name } ?: throw ProfileNotFoundException(name))

    /**
     * Attempts to create a profile with the given name and parameters
     */
    fun createProfile(
            name: String,
            targetGameVersion: String? = null,
            allowExperimental: Boolean = false,
            notifier: Notifier
    ) {
        checkNameAvailableOrThrow(name)

        val target = targetGameVersion ?: DownloadApiService.getLatestVersions().stable.headless
        notifier.update("Attempting to create profile with target version $target")
        val profile = Profile().apply {
            this.name = name
            this.targetGameVersion = target
            this.allowExperimental = allowExperimental
            this.gameVersion = getMatchingVersion(target) ?: throw MatchingVersionNotFound(target)
        }
        File("${profile.localPath}/$profileRelativeModDirectory").apply { if (!exists()) mkdirs() }
        profileSequence().add(profile)
        swapProfile(profile)
        notifier.success("Profile **$name** successfully created with version **${profile.gameVersion.versionNumber}**")
    }

    fun copyProfile(name: String, oldProfile: Profile, notifier: Notifier) {
        notifier.update("Starting profile copy...")
        checkNameAvailableOrThrow(name)
        val newProfile = Profile().apply {
            this.name = name
            this.targetGameVersion = oldProfile.targetGameVersion
            this.allowExperimental = oldProfile.allowExperimental
            this.gameVersion = oldProfile.gameVersion
        }

        val newPath = newProfile.localPath
        val oldPath = oldProfile.localPath
        val newModFolder = File("$newPath/$profileRelativeModDirectory").apply { if (!exists()) mkdirs() }
        val oldModFolder = File("$oldPath/$profileRelativeModDirectory")
        oldModFolder.apply { if (exists()) copyRecursively(newModFolder) }
        possibleConfigFiles.forEach { File("$oldPath/$it").apply { if (exists()) copyTo(File("$newPath/$it")) } }

        profileSequence().add(newProfile)
        swapProfile(newProfile)
        notifier.success("Profile **$name** successfully created as a copy of **${oldProfile.name}**")
    }

    fun updateProfile(
            profile: Profile,
            targetGameVersion: String? = profile.targetGameVersion,
            allowExperimental: Boolean = profile.allowExperimental,
            notifier: Notifier,
    ): Boolean {
        notifier.update("Starting update...", force = true)
        val target = targetGameVersion ?: DownloadApiService.getLatestVersions().stable.headless
        if (target == profile.gameVersion.versionNumber) {
            notifier.success("Profile **${profile.name}** is already at version **$target**")
            return false
        }
        profile.apply {
            this.targetGameVersion = target
            this.allowExperimental = allowExperimental
            this.gameVersion = getMatchingVersion(target) ?: throw MatchingVersionNotFound(target)
        }
        notifier.update("Cleaning obsoletes files...")
        File("${profile.localPath}/$profileRelativeModDirectory").apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }
        File("${profile.localPath}/${profile.name}-modpack.zip").apply { if (exists()) delete() }
        notifier.update("Successfully changed **${profile.name}**, running build to complete update...")
        return true
    }

    /**
     * Attempts to remove a profile with given name
     */
    fun removeProfile(profile: Profile, notifier: Notifier) {
        if (currentProfile?.name == profile.name) swapProfile(null)
        if (processManager.currentProcessProfileName == profile.name) processManager.stop()
        profileSequence().removeIf { it.id eq profile.id }
        File(profile.localPath).apply { if (exists()) deleteRecursively() }
        notifier.success("Profile **${profile.name}** successfully removed")
    }

    private fun getMatchingVersion(approxVersion: String,
                           buildType: BuildType = BuildType.HEADLESS,
                           platform: Platform = Platform.LINUX64): GameVersion? {
        val candidates = gameReleaseSequence()
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
            notifier.update("Starting download for **${version.versionNumber}**...", force = true)

        val destination = File("$baseDataDir/bin/${version.versionNumber}")
        destination.mkdirs()

        return if (downloadApiService.downloadGameClient(version.path, destination.absolutePath, notifier)) {
            version.apply { localPath = destination.absolutePath }.flushChanges()
            true
        } else
            false
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

    fun generateAuthToken(profile: Profile) {
        val now = LocalDateTime.now()

        profile.apply {
            if (token == null || tokenExpiration?.isAfter(now) != true)
                token = (1..tokenLength).map {
                    secureRandom.nextInt(tokenAllowedChars.length)
                }.map(tokenAllowedChars::get).joinToString("")
            tokenExpiration = now.plusMinutes(tokenValidityInMinutes)
        }.flushChanges()
        logger.info("Refreshed token for ${profile.name}: ${profile.token}")
    }

    fun generateModPack(profile: Profile, notifier: Notifier): String {
        val modFolder = profileRelativeModDirectory
        val fileName = "${profile.name}-modpack.zip"
        val archive = File("${profile.localPath}/$fileName").apply {
            if (exists()) {
                notifier.update("Removing old archive...", force = true)
                delete()
            }
        }.name
        val cmd = arrayOf("zip", "-r", archive, modFolder, "-x", "$modFolder/mod-settings.dat")
        notifier.update("Creating archive...")
        logger.warn("CMD: ${cmd.joinToString(" ")}")

        val p = Runtime.getRuntime().exec(cmd, null, File(profile.localPath))
        p.waitFor()
        if (p.exitValue() != 0) {
            notifier.error("Archive creation failed")
            logger.error("Command Failed : $cmd")
            logger.error("\n" + p.inputStream.bufferedReader().use { it.readText() })
            logger.error("\n" + p.errorStream.bufferedReader().use { it.readText() })
        } else
            notifier.success("Extraction successful")
        return fileName
    }

    fun editProfileConfig(notifier: Notifier) {
        val profile = currentProfileOrThrow
        generateAuthToken(profile)
        notifier.success("You can edit your profile here : ${config.server.domain}/edit/${profile.name}/${profile.token}\n$linkValidityMention")
    }

    fun listFiles(customFilter: (File) -> Boolean = { true }): List<File> {
        val profile = currentProfileOrThrow
        return File(profile.localPath).walk().maxDepth(1).toList()
                .filter { it.isFile && it.name != "mod-list.json" }
                .filter(customFilter)
    }

    fun removeFile(notifier: Notifier, fileName: String) {
        val profile = currentProfileOrThrow
        val file = File(profile.localPath + "/$fileName")
        if (file.exists()) {
            file.delete()
            notifier.success("Successfully removed file **$fileName**")
        } else {
            notifier.error("Could not remove **$fileName**, file doesn't exist")
        }
    }
}
