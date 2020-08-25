package fr.ct402.dbcfs

import fr.ct402.dbcfs.commons.AbstractComponent
import fr.ct402.dbcfs.commons.nextOrNull
import fr.ct402.dbcfs.commons.parseDateTime
import fr.ct402.dbcfs.discord.DiscordInterface
import fr.ct402.dbcfs.discord.Notifier
import fr.ct402.dbcfs.factorio.api.DownloadApiService
import fr.ct402.dbcfs.factorio.api.ModPortalApiService
import fr.ct402.dbcfs.manager.DiscordAuthManager
import fr.ct402.dbcfs.manager.ProcessManager
import fr.ct402.dbcfs.manager.ProfileManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class Command(val help: String, val run: CommandParser.(MessageReceivedEvent, List<String>) -> Unit, val depthLevel: Int = 1) {
    operator fun invoke(receiver: CommandParser, event: MessageReceivedEvent, args: List<String>) = receiver.run(event, args)
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
        else -> null
    }
    "authorize" -> Command("Adds mentionned @user and @roles to allowed whitelist", CommandParser::runAuthorizeCommand)
    "unauthorize" -> Command("Removes mentionned @user and @roles from allowed whitelist", CommandParser::runUnauthorizeCommand)
    "start" -> Command("Starts server for current profile", CommandParser::runStartCommand)
    "stop" -> Command("Stops the running process, if any", CommandParser::runStopCommand)
    "build" -> Command("Builds current profile", CommandParser::runBuildCommand)
    "swap" -> Command("Set given profile as current one", CommandParser::runSwapCommand)
    "sync" -> Command("Synchronize the game version and mod list", CommandParser::runSyncCommand)
    "test" -> Command("Used for testing features in dev", CommandParser::runTestCommand)
    "edit" -> Command("Generate edit link to setup server via URL", CommandParser::runEditCommand)
    else -> null
}

@Component
class CommandParser (
        val profileManager: ProfileManager,
        val processManager: ProcessManager,
        val downloadApiService: DownloadApiService,
        val modPortalApiService: ModPortalApiService,
        val discordAuthManager: DiscordAuthManager
): AbstractComponent() {

    fun runRemoveProfileCommand(event: MessageReceivedEvent, args: List<String>) {
        val name = args.firstOrNull() ?: return missingArgument(event)

        profileManager.removeProfile(name, Notifier(event))
    }

    fun runAuthorizeCommand(event: MessageReceivedEvent, args: List<String>) {
        discordAuthManager.addAuthorized(event.message, Notifier(event))
    }

    fun runUnauthorizeCommand(event: MessageReceivedEvent, args: List<String>) {
        discordAuthManager.removeAuthorized(event.message, Notifier(event))
    }

    fun runStartCommand(event: MessageReceivedEvent, args: List<String>) {
        val profile = profileManager.currentProfile ?: return noCurrentProfile(event)

        GlobalScope.launch {
            val msg = event.channel.sendMessage("Starting server...").complete()
            processManager.start(profile, Notifier(msg))
        }
    }

    fun runSwapCommand(event: MessageReceivedEvent, args: List<String>) {
        val name = args.firstOrNull() ?: return missingArgument(event)

        if (profileManager.swapProfile(name))
            Notifier(event).success("Current profile is now $name")
        else
            Notifier(event).error("Error: Profile $name does not exist")
    }

    fun runStopCommand(event: MessageReceivedEvent, args: List<String>) {
        if (processManager.stop())
            Notifier(event).success("Server stopped")
        else
            Notifier(event).error("Error: No process is currently running")
    }

    fun runBuildCommand(event: MessageReceivedEvent, args: List<String>) {
        val profile = profileManager.currentProfile ?: return noCurrentProfile(event)

        GlobalScope.launch {
            val notifier = Notifier(event).apply { update("Starting build...", force = true) }
            profileManager.downloadGame(notifier) && processManager.genMap(profile, notifier)
        }
    }

    fun runCreateProfileCommand(event: MessageReceivedEvent, args: List<String>) {
        val name = args.firstOrNull() ?: return missingArgument(event)
        val targetVersion = args.getOrNull(1)
        val experimental = args.getOrNull(2) == "experimental"

        if (profileManager.createProfile(name, targetVersion, experimental))
            Notifier(event).success("Profile $name created successfully")
        else
            Notifier(event).error("Error, could not create profile with given name")
    }

    fun runSyncCommand(event: MessageReceivedEvent, args: List<String>) {
        GlobalScope.launch {
            val gameSyncMessage = event.channel.sendMessage("Starting game versions sync...").complete()
            downloadApiService.syncGameVersions(Notifier(gameSyncMessage))
        }
        GlobalScope.launch {
            val modsSyncMessage = event.channel.sendMessage("Starting mods versions sync...").complete()
            modPortalApiService.syncModList(Notifier(modsSyncMessage))
        }
    }

    fun runEditCommand(event: MessageReceivedEvent, args: List<String>) {
        profileManager.currentProfile ?: return noCurrentProfile(event)
        profileManager.generateAuthToken(Notifier(event))
    }

    fun runTestCommand(event: MessageReceivedEvent, args: List<String>) {
        val dateStr = args.firstOrNull()
        if (dateStr != null) {
            val date = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME)
            logger.info("Successfully parsed $date")
        }
    }

    fun runHelpCommand(event: MessageReceivedEvent, args: List<String>) {
        val msg = if (args.isEmpty()) {
            "Usage: help <command>"
        } else {
            getCommand(args.iterator())?.help ?: "Comand ${args.reduce { acc, s -> "$acc $s" }} not found"
        }
        event.channel.sendMessage(msg).queue()
    }

    //FIXME Should be part of Notifier class
    companion object {
        fun notImplemented(event: MessageReceivedEvent) =
                Notifier(event).error("Feature not implemented yet : ${event.message.contentDisplay}")

        fun missingArgument(event: MessageReceivedEvent) =
                Notifier(event).error("Missing argument for command : ${event.message.contentDisplay}")

        fun noCurrentProfile(event: MessageReceivedEvent) =
                Notifier(event).error("No profile is currently selected, please select or create a profile first (See create profile or swap)")

        fun parseError(event: MessageReceivedEvent) =
                Notifier(event).error("Could not parse your command : ${event.message.contentDisplay}")

        fun removePrefix(str: String) =
                if (str.startsWith(commandPrefix)) str.drop(commandPrefix.length) else null
    }

    fun parseCommand(event: MessageReceivedEvent) {
        val content = event.message.contentRaw.trimStart()
        val str = removePrefix(content) ?: if (event.isFromType(ChannelType.PRIVATE)) content else return
        val tokens = str.trimStart().split(' ').filter { it.isNotEmpty() }
        val cmd = getCommand(tokens.iterator()) ?: return parseError(event)

        cmd(this, event, tokens.drop(cmd.depthLevel))
    }
}
