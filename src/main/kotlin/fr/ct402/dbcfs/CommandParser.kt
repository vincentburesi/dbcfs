package fr.ct402.dbcfs

import fr.ct402.dbcfs.commons.*
import fr.ct402.dbcfs.discord.*
import fr.ct402.dbcfs.factorio.api.DownloadApiService
import fr.ct402.dbcfs.factorio.api.ModPortalApiService
import fr.ct402.dbcfs.manager.DiscordAuthManager
import fr.ct402.dbcfs.manager.ModManager
import fr.ct402.dbcfs.manager.ProcessManager
import fr.ct402.dbcfs.manager.ProfileManager
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Command(val help: String, val run: CommandParser.(Notifier, List<String>) -> Unit, val depthLevel: Int = 1) {
    operator fun invoke(receiver: CommandParser, notifier: Notifier, args: List<String>) = receiver.run(notifier, args)
}

const val commandPrefix = "."

fun getCommand(it: Iterator<String>) = when (it.nextOrNull()) {
    "help" -> Command("This is the help command documentation, it should come into form later", CommandParser::runHelpCommand)
    "create" -> when (it.nextOrNull()) {
        "profile" -> Command("Usage: create profile <name> [<version> experimental]", CommandParser::runCreateProfileCommand, 2)
        else -> null
    }
    "remove" -> when (it.nextOrNull()) {
        "profile" -> Command("See remove-profile, remove-user, remove-file", CommandParser::runRemoveProfileCommand, 2)
        "mod" -> Command("Remove mod from current profile", CommandParser::runRemoveModCommand, 2)
        "file" -> Command("Delete file from current profile", CommandParser::runRemoveFileCommand, 2)
        else -> null
    }
    "add" -> when (it.nextOrNull()) {
        "mod" -> Command("Add mod to current profile", CommandParser::runAddModCommand, 2)
        else -> null
    }
    "info" -> Command("Display current profile summary", CommandParser::runInfoCommand)
    "list" -> when (it.nextOrNull()) {
        "profiles" -> Command("List profiles", CommandParser::runListProfilesCommand, 2)
        "mods" -> Command("List profiles", CommandParser::runListModsCommand, 2)
        "files" -> Command("List profiles", CommandParser::runListFilesCommand, 2)
        "releases" -> when (it.nextOrNull()) {
            "game" -> Command("List profiles", CommandParser::runListGameReleasesCommand, 3)
            "mod" -> Command("List profiles", CommandParser::runListModReleasesCommand, 3)
            else -> null
        }
        else -> null
    }
    "authorize" -> Command("Adds mentionned @user and @roles to allowed whitelist", CommandParser::runAuthorizeCommand)
    "unauthorize" -> Command("Removes mentionned @user and @roles from allowed whitelist", CommandParser::runUnauthorizeCommand)
    "start" -> Command("Starts server for current profile", CommandParser::runStartCommand)
    "stop" -> Command("Stops the running process, if any", CommandParser::runStopCommand)
    "build" -> Command("Builds current profile", CommandParser::runBuildCommand)
    "swap" -> Command("Set given profile as current one", CommandParser::runSwapCommand)
    "sync" -> when (it.nextOrNull()) {
        "mod" -> Command("Synchronize the given mod releases", CommandParser::runSyncModReleasesCommand, 2)
        "game" -> Command("Synchronize the game version list", CommandParser::runSyncGameReleasesCommand, 2)
        "mods" -> Command("Synchronize the mod version list", CommandParser::runSyncModsCommand, 2)
        "all", null -> Command("Synchronize the game version and mod list", CommandParser::runSyncAllCommand) //no args, ignore depth
        else -> null
    }
    "test" -> Command("Used for testing features in dev", CommandParser::runTestCommand)
    "edit" -> Command("Generate edit link to setup server via URL", CommandParser::runEditCommand)
    "revoke" -> Command("Cancels current profile token", CommandParser::runRevokeCommand)
    else -> null
}

@Component
class CommandParser(
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
        profileManager.generateAuthToken(profile, notifier)
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

    fun runTestCommand(notifier: Notifier, args: List<String>) {
        val dateStr = args.firstOrNull()
        if (dateStr != null) {
            val date = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME)
            logger.info("Successfully parsed $date")
        }
    }

    fun runHelpCommand(notifier: Notifier, args: List<String>) {
        val msg = if (args.isEmpty()) {
            "Usage: help <command>"
        } else {
            getCommand(args.iterator())?.help ?: "Comand ${args.reduce { acc, s -> "$acc $s" }} not found"
        }
        notifier.success(msg)
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
