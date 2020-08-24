package fr.ct402.dbcfs

import fr.ct402.dbcfs.commons.AbstractComponent
import fr.ct402.dbcfs.commons.nextOrNull
import fr.ct402.dbcfs.discord.Notifier
import fr.ct402.dbcfs.factorio.api.DownloadApiService
import fr.ct402.dbcfs.factorio.api.ModPortalApiService
import fr.ct402.dbcfs.manager.ProcessManager
import fr.ct402.dbcfs.manager.ProfileManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

class Command(val help: String, val run: CommandParser.(MessageReceivedEvent, List<String>) -> Unit, val depthLevel: Int = 1) {
    operator fun invoke(receiver: CommandParser, event: MessageReceivedEvent, args: List<String>) = receiver.run(event, args)
}

const val commandPrefix = "."
const val noProfileSelectedMessage = "No profile is currently selected, please select or create a profile first (See create profile or swap)"

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
    "start" -> Command("Starts server for current profile", CommandParser::runStartCommand)
    "stop" -> Command("Stops the running process, if any", CommandParser::runStopCommand)
    "build" -> Command("Builds current profile", CommandParser::runBuildCommand)
    "swap" -> Command("Set given profile as current one", CommandParser::runSwapCommand)
    "sync" -> Command("Synchronize the game version and mod list", CommandParser::runSyncCommand)
    "test" -> Command("Used for testing features in dev", CommandParser::runTestCommand)
    else -> null
}

@Component
class CommandParser (
        val profileManager: ProfileManager,
        val processManager: ProcessManager,
        val downloadApiService: DownloadApiService,
        val modPortalApiService: ModPortalApiService
): AbstractComponent() {

    fun runRemoveProfileCommand(event: MessageReceivedEvent, args: List<String>) {
        val name = args.firstOrNull() ?: return missingArgument(event)
        val result = profileManager.removeProfile(name)
        val msg = if (result) "Profile $name successfully removed" else "Error, could not find profile with given name"
        event.channel.sendMessage(msg).queue()
    }

    fun runStartCommand(event: MessageReceivedEvent, args: List<String>) {
        val profile = profileManager.currentProfile
        if (profile == null) {
            event.channel.sendMessage(noProfileSelectedMessage).queue()
            return
        } else
            GlobalScope.launch {
                val msg = event.channel.sendMessage("Starting server").complete()
                processManager.start(profile, Notifier(msg))
            }
    }

    fun runSwapCommand(event: MessageReceivedEvent, args: List<String>) {
        val name = args.firstOrNull()
        val msg = if (name != null && profileManager.swapProfile(name))
            "Current profile is now $name"
        else
            "Error: Could not swap profile"
        event.channel.sendMessage(msg).queue()
    }

    fun runStopCommand(event: MessageReceivedEvent, args: List<String>) {
        val result = processManager.stop()
        val msg = if (result) "Server stopped" else "Error: No process is currently running"
        event.channel.sendMessage(msg).queue()
    }

    fun runBuildCommand(event: MessageReceivedEvent, args: List<String>) {
        val profile = profileManager.currentProfile
        val msg = if (profile != null) {
            val result = profileManager.downloadGame() && processManager.genMap(profile)
            if (result) "Profile is ready, you can start the server." else "Error : Could not download the game. See logs for more informations"
        } else "No profile is currently selected, please select or create a profile first (See create profile or swap)"
        event.channel.sendMessage(msg).queue()
    }

    fun runCreateProfileCommand(event: MessageReceivedEvent, args: List<String>) {
        val name = args.firstOrNull() ?: return missingArgument(event)
        val targetVersion = args.getOrNull(1)
        val experimental = args.getOrNull(2) == "experimental"
        val result = profileManager.createProfile(name, targetVersion, experimental)
     val msg = if (result) "Profile $name created successfully" else "Error, could not create profile with given name"
        event.channel.sendMessage(msg).queue()
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

    fun runTestCommand(event: MessageReceivedEvent, args: List<String>) {
        val argsConcat = args.run { if (isEmpty()) "" else reduce { acc, s -> "$acc $s" } }
        val msg = "Le parsing des commandes fonctionne :\n$argsConcat"
        val sent = event.channel.sendMessage(msg).complete()
        sent?.editMessage(sent.contentRaw + "\n Et je peux edit mes propres messages !")
                ?.queueAfter(5L, TimeUnit.SECONDS)
    }

    fun runHelpCommand(event: MessageReceivedEvent, args: List<String>) {
        val msg = if (args.isEmpty()) {
            "Usage: help <command>"
        } else {
            getCommand(args.iterator())?.help ?: "Comand ${args.reduce { acc, s -> "$acc $s" }} not found"
        }
        event.channel.sendMessage(msg).queue()
    }

    fun notImplemented(event: MessageReceivedEvent) {
        val msg = "Feature not implemented yet : ${event.message.contentDisplay}"
        event.channel.sendMessage(msg).queue()
    }

    fun missingArgument(event: MessageReceivedEvent) {
        val msg = "Missing argument for command : ${event.message.contentDisplay}"
        event.channel.sendMessage(msg).queue()
    }

    fun parseError(event: MessageReceivedEvent) {
        val msg = "Could not parse your command : ${event.message.contentDisplay}"
        event.channel.sendMessage(msg).queue()
    }

    fun removePrefix(str: String) =
            if (str.startsWith(commandPrefix)) str.drop(commandPrefix.length) else null

    fun parseCommand(event: MessageReceivedEvent) {
        val content = event.message.contentRaw.trimStart()
        val str = removePrefix(content) ?: if (event.isFromType(ChannelType.PRIVATE)) content else return
        val tokens = str.trimStart().split(' ').filter { it.isNotEmpty() }
        val cmd = getCommand(tokens.iterator()) ?: return parseError(event)

        cmd(this, event, tokens.drop(cmd.depthLevel))
    }
}
