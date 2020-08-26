package fr.ct402.dbcfs

import fr.ct402.dbcfs.commons.*
import fr.ct402.dbcfs.discord.Notifier
import fr.ct402.dbcfs.discord.printGameReleases
import fr.ct402.dbcfs.discord.printModReleases
import fr.ct402.dbcfs.discord.printProfiles
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
//        "files" -> Command("List profiles", CommandParser::runListFilesCommand, 2)
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
        "mod" -> Command("Synchronize the given mod releases", CommandParser::runSyncModCommand, 2)
        null -> Command("Synchronize the game version and mod list", CommandParser::runSyncCommand)
        else -> null
    }
    "test" -> Command("Used for testing features in dev", CommandParser::runTestCommand)
    "edit" -> Command("Generate edit link to setup server via URL", CommandParser::runEditCommand)
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
) : AbstractComponent() {

    fun runInfoCommand(notifier: Notifier, args: List<String>) {
        val profile = profileManager.currentProfileOrThrow
        notifier.update("Fetching data...", force = true)
        val nbMods = modManager.getModReleaseListByProfile(profile).size
        val msg = "Current profile is ${profile.name}\nVersion ${profile.gameVersion.versionNumber} " +
                (if (profile.gameVersion.localPath != null) " installed\n" else "\n") +
                (if (profile.allowExperimental) "*experimental updates are enabled*\n" else "\n") +
                (if (nbMods != 0) "*$nbMods mod${ if (nbMods == 1) " is" else "s are" } currently installed*" else "")
        notifier.print(msg)
    }

    fun runListProfilesCommand(notifier: Notifier, args: List<String>) =
            notifier printProfiles profileManager.getAllProfiles().sortedBy { it.name }

    fun runListModsCommand(notifier: Notifier, args: List<String>) {
        val name = args.firstOrNull()
        val profile = if (name != null) profileManager.getProfileByNameOrThrow(name) else profileManager.currentProfileOrThrow
        notifier printModReleases modManager.getModReleaseListByProfile(profile)
    }

    fun runListGameReleasesCommand(notifier: Notifier, args: List<String>) =
            notifier printGameReleases profileManager.getAllGameReleases().groupBy { it.versionNumber }.map { it.value.first() }

    fun runListModReleasesCommand(notifier: Notifier, args: List<String>) {
        val modName = args.firstOrNull() ?: throw MissingArgumentException("list releases mod", "name")
        notifier printModReleases modManager.getModReleaseListByName(modName)
    }

    fun runRemoveProfileCommand(notifier: Notifier, args: List<String>) =
            profileManager.removeProfile(args.firstOrNull()
                    ?: throw MissingArgumentException("remove", "name"), notifier)

    fun runAuthorizeCommand(notifier: Notifier, args: List<String>) =
            discordAuthManager.addAuthorized(notifier.event.message, notifier)

    fun runUnauthorizeCommand(notifier: Notifier, args: List<String>) =
            discordAuthManager.removeAuthorized(notifier.event.message, notifier)

    fun runStartCommand(notifier: Notifier, args: List<String>) = notifier.launchAsCoroutine {
        processManager.start(profileManager.currentProfileOrThrow, notifier)
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
            profileManager.downloadGame(notifier) && processManager.genMap(profile, notifier)
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

    fun runAddModCommand(notifier: Notifier, args: List<String>) {
        val modName = args.firstOrNull() ?: throw MissingArgumentException("add mod", "name")
        val version = args.getOrNull(1)
        notifier.update("Trying to add $modName...", force = true)
        if (version == null)
            modManager.addMod(notifier, modName)
        else
            modManager.addMod(notifier, modName, version)
    }

    fun runSyncModCommand(notifier: Notifier, args: List<String>) {
        notifier.launchAsCoroutine {
            val name = args.firstOrNull() ?: throw MissingArgumentException("sync mod", "name")
            val mod = modManager.getModByNameOrThrow(name)

            modPortalApiService.syncModReleaseList(mod, notifier)
        }
    }

    fun runSyncCommand(notifier: Notifier, args: List<String>) {
        notifier.launchAsCoroutine {
            downloadApiService.syncGameVersions(notifier)
        }
        notifier.launchAsCoroutine {
            modPortalApiService.syncModList(Notifier(notifier.event))
        }
    }

    fun runEditCommand(notifier: Notifier, args: List<String>) {
        profileManager.currentProfileOrThrow
        profileManager.generateAuthToken(notifier)
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
            e.multicatch(
                    NoCurrentProfileException::class,
                    ProfileNotFoundException::class,
                    MissingArgumentException::class,
            ) {
                notifier print e
            }
        }
    }
}
