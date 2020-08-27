package fr.ct402.dbcfs.commons

import fr.ct402.dbcfs.discord.*
import fr.ct402.dbcfs.factorio.api.DownloadApiService
import fr.ct402.dbcfs.factorio.api.ModPortalApiService
import fr.ct402.dbcfs.manager.DiscordAuthManager
import fr.ct402.dbcfs.manager.ModManager
import fr.ct402.dbcfs.manager.ProcessManager
import fr.ct402.dbcfs.manager.ProfileManager
import fr.ct402.dbcfs.persist.model.GameVersion
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class CommandRunner(
        val profileManager: ProfileManager,
        val processManager: ProcessManager,
        val downloadApiService: DownloadApiService,
        val modPortalApiService: ModPortalApiService,
        val discordAuthManager: DiscordAuthManager,
        val modManager: ModManager,
        val config: Config,
) : AbstractComponent() {

    fun runInfoCommand(notifier: Notifier, args: List<String>) {
        val profile = profileManager.currentProfileOrThrow
        notifier.update("Fetching data...", force = true)
        val nbMods = modManager.getModReleaseListByProfile(profile).size
        val nbSaves = profileManager.listFiles { it.extension == "zip" }.size
        val msg = "Current profile is ${profile.name}\nVersion ${profile.gameVersion.versionNumber} " +
                (if (profile.gameVersion.localPath != null) " installed" else "") +
                (if (profile.allowExperimental) "\n*experimental updates are enabled*" else "") +
                (if (nbMods != 0) "\n*$nbMods mod${ if (nbMods == 1) " is" else "s are" } currently installed*" else "") +
                (if (nbSaves != 0) "\n*$nbSaves save${ if (nbMods == 1) " is" else "s are" } currently present*" else "") //TODO ignore all zip package files

        notifier.print(msg)
    }

    fun runListProfilesCommand(notifier: Notifier, args: List<String>) =
            notifier printProfiles profileManager.getAllProfiles().sortedBy { it.name }

    fun runListModsCommand(notifier: Notifier, args: List<String>) {
        val name = args.firstOrNull()
        val profile = if (name != null) profileManager.getProfileByNameOrThrow(name) else profileManager.currentProfileOrThrow
        notifier.printModReleases(modManager.getModReleaseListByProfile(profile))
    }

    fun runListFilesCommand(notifier: Notifier, args: List<String>) {
        val profile = profileManager.currentProfileOrThrow
        profileManager.generateAuthToken(profile)
        notifier.printProfileFiles(profileManager.listFiles(), profile, config.server.domain)
    }

    fun runListGameReleasesCommand(notifier: Notifier, args: List<String>) =
            notifier printGameReleases profileManager.getAllGameReleases().groupBy { it.versionNumber }.map { it.value.first() }

    fun runListModReleasesCommand(notifier: Notifier, args: List<String>) {
        val modName = args.firstOrNull() ?: throw MissingArgumentException("list releases mod", "name")
        val all = args.getOrNull(1) == "all"
        notifier.printModReleases(modManager.getModReleaseListByName(modName), all)
    }

    fun runRemoveProfileCommand(notifier: Notifier, args: List<String>) {
        val name = args.firstOrNull() ?: throw MissingArgumentException("remove", "name")
        val profile = profileManager.getProfileByNameOrThrow(name)

        modManager.removeAllModsFromProfile(profile, notifier)
        profileManager.removeProfile(profile, notifier)
    }

    fun runGetClientCommand(notifier: Notifier, args: List<String>) {
        notifier.launchAsCoroutine {
            val profile = profileManager.currentProfileOrThrow
            val platform = when (args.firstOrNull()) {
                "osx" -> GameVersion.Platform.OSX
                "linux32" -> GameVersion.Platform.LINUX32
                "linux", "linux64" -> GameVersion.Platform.LINUX64
                "win32" -> GameVersion.Platform.WIN32
                "win", "win64", null -> GameVersion.Platform.WIN64
                else -> throw InvalidArgumentException("platform", "**win64 (default)**, win32, linux64, linux32, osx")
            }
            notifier.update("Searching for matching release...", force = true)
            val release = profileManager.getAllGameReleasesForProfile(profile).find { it.platform == platform }
                    ?: throw GameReleaseNotFoundException()

            val fileName = downloadApiService.downloadToProfile(release.path, profile, notifier)
            profileManager.generateAuthToken(profile)
            notifier.printProfileFiles(listOf(fileName), profile, config.server.domain)
        }
    }

    fun runAuthorizeCommand(notifier: Notifier, args: List<String>) =
            discordAuthManager.addAuthorized(notifier.event.message, notifier)

    fun runUnauthorizeCommand(notifier: Notifier, args: List<String>) =
            discordAuthManager.removeAuthorized(notifier.event.message, notifier)

    fun runStartCommand(notifier: Notifier, args: List<String>) = notifier.launchAsCoroutine {
        val save = args.firstOrNull()
        processManager.start(profileManager.currentProfileOrThrow, notifier, save)
    }

    fun runSwapCommand(notifier: Notifier, args: List<String>) {
        val name = args.firstOrNull() ?: throw MissingArgumentException("swap", "name")

        profileManager.swapProfile(name)
        notifier.success("Current profile is now $name")
    }

    fun runStopCommand(notifier: Notifier, args: List<String>) {
        if (processManager.stop())
            notifier.success("Server stopped")
        else
            notifier.error("Error: No process is currently running")
    }

    fun runBuildCommand(notifier: Notifier, args: List<String>) {
        val profile = profileManager.currentProfileOrThrow

        notifier.launchAsCoroutine {
            notifier.update("Starting build...", force = true)
            profileManager.downloadGame(notifier) && modManager.downloadMods(profile, notifier) && processManager.genMap(profile, notifier)
        }
    }

    fun runCreateProfileCommand(notifier: Notifier, args: List<String>) {
        val name = args.firstOrNull() ?: throw MissingArgumentException("create profile", "name")
        val targetVersion = args.getOrNull(1)
        val experimental = args.getOrNull(2) == "experimental"

        profileManager.createProfile(name, targetVersion, experimental, notifier)
    }

    fun runRemoveModCommand(notifier: Notifier, args: List<String>) {
        val modName = args.firstOrNull() ?: throw MissingArgumentException("remove mod", "name")
        notifier.update("Trying to remove $modName...", force = true)
        modManager.removeMod(notifier, modName)
    }

    fun runRemoveFileCommand(notifier: Notifier, args: List<String>) {
        val fileName = args.firstOrNull() ?: throw MissingArgumentException("remove file", "name")
        profileManager.removeFile(notifier, fileName)
    }

    fun runAddModCommand(notifier: Notifier, args: List<String>) {
        val modName = args.firstOrNull() ?: throw MissingArgumentException("add mod", "name")
        val version = args.getOrNull(1)
        notifier.update("Trying to add $modName...", force = true)
        if (version == null)
            modManager.addMod(notifier, modName)
        else
            modManager.addMod(notifier, modName, version)
    }

    fun runSyncModReleasesCommand(notifier: Notifier, args: List<String>) {
        notifier.launchAsCoroutine {
            val name = args.firstOrNull() ?: throw MissingArgumentException("sync mod", "name")
            val mod = modManager.getModByNameOrThrow(name)

            modPortalApiService.syncModReleaseList(mod, notifier)
        }
    }

    fun runSyncGameReleasesCommand(notifier: Notifier, args: List<String>) {
        notifier.launchAsCoroutine {
            downloadApiService.syncGameVersions(notifier)
        }
    }

    fun runSyncModsCommand(notifier: Notifier, args: List<String>) {
        notifier.launchAsCoroutine {
            modPortalApiService.syncModList(notifier)
        }
    }

    fun runSyncAllCommand(notifier: Notifier, args: List<String>) {
        runSyncGameReleasesCommand(notifier, args)
        runSyncModsCommand(Notifier(notifier.event), args)
    }

    fun runEditCommand(notifier: Notifier, args: List<String>) =
            profileManager.editProfileConfig(notifier)

    fun runRevokeCommand(notifier: Notifier, args: List<String>) {
        val profile = profileManager.currentProfileOrThrow.apply { invalidateToken() }
        notifier.success("Successfully revoked token for ${profile.name}, all related links are now invalid")
    }

    fun runCurrentCommand(notifier: Notifier, args: List<String>) =
            notifier.print(profileManager.currentProfile?.name ?: "*No profile selected*", force = true)

    fun runTestCommand(notifier: Notifier, args: List<String>) {
        val dateStr = args.firstOrNull()
        if (dateStr != null) {
            val date = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME)
            logger.info("Successfully parsed $date")
        }
    }

    fun runHelpCommand(notifier: Notifier, args: List<String>) {
        val msg = if (args.isEmpty()) {
            "Usage: help COMMAND"
        } else {
            getCommand(args.iterator())?.help ?: "Comand ${args.reduce { acc, s -> "$acc $s" }} not found"
        }
        notifier.print(msg, force = true)
    }

    //FIXME Should be part of Notifier class
    companion object {
        fun notImplemented(notifier: Notifier) =
                notifier.error("Feature not implemented yet : ${notifier.event.message.contentDisplay}")

        fun removePrefix(str: String) =
                if (str.startsWith(commandPrefix)) str.drop(commandPrefix.length) else null
    }

    fun parseCommand(event: MessageReceivedEvent) {
        val content = event.message.contentRaw.trimStart()
        val str = removePrefix(content) ?: if (event.isFromType(ChannelType.PRIVATE)) content else return
        val tokens = str.trimStart().split(' ').filter { it.isNotEmpty() }
        val notifier = Notifier(event)
        val cmd = getCommand(tokens.iterator()) ?: return notifier.parseError()

        try {
            cmd(this, notifier, tokens.drop(cmd.depthLevel))
        } catch (e: Exception) {
            notifier print e
        }
    }
}
