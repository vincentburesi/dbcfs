package fr.ct402.dbcfs.refactor.commons

import fr.ct402.dbcfs.Notifier

class Command(val help: String, val run: CommandRunner.(Notifier, List<String>) -> Unit, val depthLevel: Int = 1) {
    operator fun invoke(receiver: CommandRunner, notifier: Notifier, args: List<String>) = receiver.run(notifier, args)
}

fun getCommand(it: Iterator<String>) = when (it.nextOrNull()) {
    "help" -> Command("**help** __other command__\n*Print other commands usage*\nSyntax: **command** __argument__ [__optional_argument__] litteral_value possible|values", CommandRunner::runHelpCommand)
    "create" -> when (it.nextOrNull()) {
        "profile" -> Command("**create profile** __name__ [__version__] [experimental]\n*Create new profile, default is latest version and disallow experimental releases*", CommandRunner::runCreateProfileCommand, 2)
        else -> null
    }
    "copy" -> when (it.nextOrNull()) {
        "profile" -> Command("**copy profile** __new_name__\n*Create new profile as a duplicate from active one*", CommandRunner::runCopyProfileCommand, 2)
        else -> null
    }
    "remove" -> when (it.nextOrNull()) {
        "profile" -> Command("**remove profile** __name__\n*Remove profile and associated files*", CommandRunner::runRemoveProfileCommand, 2)
        "mod" -> Command("**remove mod** __name__\n*Remove given mod from active profile*", CommandRunner::runRemoveModCommand, 2)
        "file" -> Command("**remove file** __name__\n*Delete file from active profile*", CommandRunner::runRemoveFileCommand, 2)
        else -> null
    }
    "add" -> when (it.nextOrNull()) {
        "mod" -> Command("**add mod** __name__ [__version__]\n*Add mod to active profile (default is latest version)*", CommandRunner::runAddModCommand, 2)
        else -> null
    }
    "info" -> Command("**info**\n*Display current profile summary*", CommandRunner::runInfoCommand)
    "list" -> when (it.nextOrNull()) {
        "profiles" -> Command("**list profiles**\n*List available profiles*", CommandRunner::runListProfilesCommand, 2)
        "mods" -> Command("**list mods**\n*List all mods from active profile*", CommandRunner::runListModsCommand, 2)
        "files" -> Command("**list files**\n*List all files associated to active profile*", CommandRunner::runListFilesCommand, 2)
        "releases" -> when (it.nextOrNull()) {
            "game" -> Command("**list releases game**\n*List all available game releases (synchronized from Factorio servers)*", CommandRunner::runListGameReleasesCommand, 3)
            "mod" -> Command("**list releases mod** __name__ [all]\n*List all available releases from a given mod (truncated on first entries by default)*", CommandRunner::runListModReleasesCommand, 3)
            else -> null
        }
        else -> null
    }
    "get" -> when (it.nextOrNull()) {
        "client" -> Command("**get client** [win|win64|win32|linux|linux64|linux32|osx] [alpha|headless]\n*Obtain game client download link for active profile and target platform/build type (win64 and alpha by default)*", CommandRunner::runGetClientCommand, 2)
        "mods" -> Command("**get mods**\n*Obtain archive download link with all mods associated with active profile*", CommandRunner::runGetModPackCommand, 2)
        else -> null
    }
    "allow" -> Command("**allow** __mentions__\n*Adds mentionned @users, @roles and #channels to allowed whitelist*", CommandRunner::runAllowCommand)
    "disallow" -> Command("**disallow** __mentions__\n*Removes mentionned @users, @roles and #channels from allowed whitelist*", CommandRunner::runDisallowCommand)
    "start" -> Command("**start**\n*Starts server for active profile*", CommandRunner::runStartCommand)
    "stop" -> Command("**stop**\n*Stops the running process, if any*", CommandRunner::runStopCommand)
    "build" -> Command("**build**\n*Builds current profile: download game and mod files then generate the map*", CommandRunner::runBuildCommand)
    "update" -> when (it.nextOrNull()) {
        "profile" -> Command("**update profile** [__version__]\n*Update the active profile*", CommandRunner::runUpdateProfileCommand, 2)
        "mod" -> Command("**update mod** [__version__]\n*Update the given mod associated with the active profile*", CommandRunner::runUpdateModCommand, 2)
        "all" -> Command("**update all**\n*Updates every component associated to the active profile to the latest version*", CommandRunner::runUpdateAllCommand, 2)
        else -> null
    }
    "swap" -> Command("**swap** __profile__\n*Set given profile as the active one*", CommandRunner::runSwapCommand)
    "sync" -> when (it.nextOrNull()) {
        "mod" -> Command("**sync mod** __name__\n*Retrieve the given mod list of releases*", CommandRunner::runSyncModReleasesCommand, 2)
        "game" -> Command("**sync game**\n*Synchronize the game releases list with Factorio servers*", CommandRunner::runSyncGameReleasesCommand, 2)
        "mods" -> Command("**sync mods**\n*Synchronize the mod list with Factorio Mod Portal*\n__**This command blocks the bot while processing and can take a long time to finish**__", CommandRunner::runSyncModsCommand, 2)
        "all", null -> Command("**sync**|**sync all**\n*Synchronize the game versions and mod list*\n__**This command blocks the bot while processing and can take a long time to finish**__", CommandRunner::runSyncAllCommand) //no args, ignore depth
        else -> null
    }
    "test" -> Command("**test**\n*Used for test purposes*", CommandRunner::runTestCommand)
    "edit" -> Command("**edit**\n*Generate an edit link for active profile*", CommandRunner::runEditCommand)
    "revoke" -> Command("**revoke**\n*Cancels current profile authentication token (This will invalidate all related URLs)*", CommandRunner::runRevokeCommand)
    "generate-stellaris-builds" -> Command("**generate-stellaris-builds __players__**\nThis will generate a Stellaris random build for everyone and display a rule reminder**", CommandRunner::runGenerateStellarisBuildsCommand)
    else -> null
}

const val listOfAvailableCommands = """
:gear:    __**Welcome to DBCFS**__    :gear:

:small_blue_diamond: **help** *Displays command documentation*
:small_blue_diamond: **info** *Display a short status*
:small_blue_diamond: **swap** *Select active profile*
:small_blue_diamond: **allow|disallow** *Manage who can use this bot*
:small_blue_diamond: **sync game|mod|mods|all** *Starts sync with Factorio servers (can take some time)*
:small_blue_diamond: **list profiles|mods|files|releases game|releases mod** *Lists various elements*
:small_blue_diamond: **create|copy|remove profile** *Manage profiles*
:small_blue_diamond: **edit** *Open profile HTML configuration*
:small_blue_diamond: *You can also directly upload configuration files to Discord*
:small_blue_diamond: **build|start|stop** *Manage the game process*
:small_blue_diamond: **add|remove mod** *Manage active profile mods*
:small_blue_diamond: **get client|mods** *Obtain download links*
:small_blue_diamond: **remove file** *Remove server-side files*
:small_blue_diamond: **update profile|mod|all** *Update versions*
:small_blue_diamond: **revoke** *Cancel auth token*
:small_blue_diamond: **generate-stellaris-builds** *Generate Stellaris builds for human games*
:small_blue_diamond: **test** ||*Used for test purposes* :question::interrobang:||
"""
