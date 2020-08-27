package fr.ct402.dbcfs.commons

import fr.ct402.dbcfs.discord.Notifier

class Command(val help: String, val run: CommandRunner.(Notifier, List<String>) -> Unit, val depthLevel: Int = 1) {
    operator fun invoke(receiver: CommandRunner, notifier: Notifier, args: List<String>) = receiver.run(notifier, args)
}

const val commandPrefix = "."

fun getCommand(it: Iterator<String>) = when (it.nextOrNull()) {
    "help" -> Command("This is the help command documentation, it should come into form later", CommandRunner::runHelpCommand)
    "create" -> when (it.nextOrNull()) {
        "profile" -> Command("Usage: create profile <name> [<version> experimental]", CommandRunner::runCreateProfileCommand, 2)
        else -> null
    }
    "copy" -> when (it.nextOrNull()) {
        "profile" -> Command("Usage: copy profile <new_name>", CommandRunner::runCopyProfileCommand, 2)
        else -> null
    }
    "remove" -> when (it.nextOrNull()) {
        "profile" -> Command("See remove-profile, remove-user, remove-file", CommandRunner::runRemoveProfileCommand, 2)
        "mod" -> Command("Remove mod from current profile", CommandRunner::runRemoveModCommand, 2)
        "file" -> Command("Delete file from current profile", CommandRunner::runRemoveFileCommand, 2)
        else -> null
    }
    "add" -> when (it.nextOrNull()) {
        "mod" -> Command("Add mod to current profile", CommandRunner::runAddModCommand, 2)
        else -> null
    }
    "info" -> Command("Display current profile summary", CommandRunner::runInfoCommand)
    "list" -> when (it.nextOrNull()) {
        "profiles" -> Command("List profiles", CommandRunner::runListProfilesCommand, 2)
        "mods" -> Command("List all mods from current profile", CommandRunner::runListModsCommand, 2)
        "files" -> Command("List all files available from current profile", CommandRunner::runListFilesCommand, 2)
        "releases" -> when (it.nextOrNull()) {
            "game" -> Command("List all available game releases", CommandRunner::runListGameReleasesCommand, 3)
            "mod" -> Command("List all available releases from a certain mod (truncated on first entries by default)", CommandRunner::runListModReleasesCommand, 3)
            else -> null
        }
        else -> null
    }
    "get" -> when (it.nextOrNull()) {
        "client" -> Command("Get game client download link for current profile, can specify platform (**win64 -default-**, win32, linux64, linux32 or osx)", CommandRunner::runGetClientCommand, 2)
        "mods" -> Command("Get mods archive download link for current profile", CommandRunner::runGetModPackCommand, 2)
        else -> null
    }
    "allow" -> Command("Adds mentionned @user and @roles to allowed whitelist", CommandRunner::runAllowCommand)
    "disallow" -> Command("Removes mentionned @user and @roles from allowed whitelist", CommandRunner::runDisallowCommand)
    "start" -> Command("Starts server for current profile", CommandRunner::runStartCommand)
    "stop" -> Command("Stops the running process, if any", CommandRunner::runStopCommand)
    "build" -> Command("Builds current profile", CommandRunner::runBuildCommand)
    "update" -> Command("Changes the current profile's version", CommandRunner::runUpdateCommand)
    "swap" -> Command("Set given profile as current one", CommandRunner::runSwapCommand)
    "sync" -> when (it.nextOrNull()) {
        "mod" -> Command("Synchronize the given mod releases", CommandRunner::runSyncModReleasesCommand, 2)
        "game" -> Command("Synchronize the game version list", CommandRunner::runSyncGameReleasesCommand, 2)
        "mods" -> Command("Synchronize the mod version list", CommandRunner::runSyncModsCommand, 2)
        "all", null -> Command("Synchronize the game version and mod list", CommandRunner::runSyncAllCommand) //no args, ignore depth
        else -> null
    }
    "test" -> Command("Used for testing features in dev", CommandRunner::runTestCommand)
    "edit" -> Command("Generate edit link to setup server via URL", CommandRunner::runEditCommand)
    "revoke" -> Command("Cancels current profile token", CommandRunner::runRevokeCommand)
    else -> null
}
