package fr.ct402.dbcfs.refactor.commons

import fr.ct402.dbcfs.*
import fr.ct402.dbcfs.refactor.factorio.api.DownloadApiService
import fr.ct402.dbcfs.refactor.factorio.api.ModPortalApiService
import fr.ct402.dbcfs.refactor.manager.DiscordAuthManager
import fr.ct402.dbcfs.refactor.manager.ModManager
import fr.ct402.dbcfs.refactor.manager.ProcessManager
import fr.ct402.dbcfs.refactor.manager.ProfileManager
import fr.ct402.dbcfs.persist.model.GameVersion
import fr.ct402.dbcfs.listPoint
import fr.ct402.dbcfs.utilities.StellarisManager
import kotlinx.coroutines.delay
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.springframework.stereotype.Component

@Component
class CommandRunner(
        val profileManager: ProfileManager,
        val processManager: ProcessManager,
        val downloadApiService: DownloadApiService,
        val modPortalApiService: ModPortalApiService,
        val discordAuthManager: DiscordAuthManager,
        val modManager: ModManager,
        val stellarisManager: StellarisManager,
        val config: Config,
) : AbstractComponent() {

    fun runInfoCommand(notifier: Notifier, args: List<String>) {
        val profile = profileManager.currentProfileOrThrow
        notifier.running("Fetching data...").queue()
        val nbMods = modManager.getModReleaseListByProfile(profile).size
        val nbSaves = profileManager.listFiles { it.extension == "zip" && !it.nameWithoutExtension.endsWith("-modpack") }.size
        val msg = "Current profile is **${profile.name}**\nVersion **${profile.gameVersion.versionNumber}**" +
                (if (profile.gameVersion.localPath != null) " is installed" else "") +
                (if (profile.allowExperimental) "\n*experimental updates are enabled*" else "") +
                (if (nbMods != 0) "\n*$nbMods mod${ if (nbMods == 1) " is" else "s are" } currently installed*" else "") +
                (if (nbSaves != 0) "\n*$nbSaves save${ if (nbSaves == 1) " is" else "s are" } currently present*" else "")

        notifier.success(msg).queue()
    }

    fun runListProfilesCommand(notifier: Notifier, args: List<String>) {
        val strings = profileManager.getAllProfiles()
                .sortedBy { it.name }
                .map {
                    (if (it.allowExperimental) ":tools:" else ":shield:") +
                            " **${it.name}** - ${it.gameVersion.versionNumber}" +
                            (if (it.gameVersion.localPath != null) " :floppy_disk:" else "")
                }

        notifier.printFullList(strings)
    }

    fun runListModsCommand(notifier: Notifier, args: List<String>) {
        val name = args.firstOrNull()
        val profile = if (name != null) profileManager.getProfileByNameOrThrow(name) else profileManager.currentProfileOrThrow

        notifier.printModReleases(modManager.getModReleaseListByProfile(profile))
    }

    fun runListFilesCommand(notifier: Notifier, args: List<String>) {
        val profile = profileManager.currentProfileOrThrow
        profileManager.generateAuthToken(profile)
        val strings = profileManager.listFiles()
                .map {
                    "$listPoint **${it.name}** " +
                            "*${fileSizeAsString(it.length())}* " +
                            "${config.server.domain}/file/${profile.name}/${profile.token}/${it.name}"
                }

        notifier.printFullList(strings + "*Links will be valid for the next $tokenValidityInMinutes minutes*")
    }

    fun runListGameReleasesCommand(notifier: Notifier, args: List<String>) {
        val all = args.getOrNull(1) == "all"
        val strings = profileManager.getAllGameReleases()
                .groupBy { it.versionNumber }
                .map { it.value.first() }
                .map {
                    (if (!it.isStable) ":tools:" else ":shield:") + " **${it.versionNumber}** "
                }

        if (all) notifier.printFullList(strings) else notifier.printTruncatedList(strings)
    }

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
            val buildType = when(args.getOrNull(1)) {
                "headless" -> GameVersion.BuildType.HEADLESS
                "alpha", null -> GameVersion.BuildType.ALPHA
                else -> throw InvalidArgumentException("buildType", "**alpha (default)**, headless")
            }

            notifier.running("Searching for matching release...").queue()
            val release = profileManager.getAllGameReleasesForProfile(profile)
                    .filter { it.buildType == buildType }
                    .find { it.platform == platform }
                    ?: throw GameReleaseNotFoundException()

            val fileName = downloadApiService.downloadToProfile(release.path, profile, notifier)
            profileManager.generateAuthToken(profile)
            notifier.success(fileName, profile, config.server.domain).queue()
        }
    }

    fun runGetModPackCommand(notifier: Notifier, args: List<String>) {
        notifier.launchAsCoroutine {
            val profile = profileManager.currentProfileOrThrow
            notifier.running("Generating modpack file...").queue()
            modManager.downloadMods(profile, notifier)
            val fileName = profileManager.generateModPack(profile, notifier)
            profileManager.generateAuthToken(profile)
            notifier.success(fileName, profile, config.server.domain).queue()
        }
    }

    fun runAllowCommand(notifier: Notifier, args: List<String>) =
            discordAuthManager.allowFromMentions(notifier.event.message, notifier)

    fun runDisallowCommand(notifier: Notifier, args: List<String>) =
            discordAuthManager.disallowFromMentions(notifier.event.message, notifier)

    fun runStartCommand(notifier: Notifier, args: List<String>) = notifier.launchAsCoroutine {
        val save = args.firstOrNull()
        processManager.start(profileManager.currentProfileOrThrow, notifier, save)
    }

    fun runUpdateProfileCommand(notifier: Notifier, args: List<String>) {
        val profile = profileManager.currentProfileOrThrow
        if (profileManager.updateProfile(profile, args.firstOrNull(),
                        args.getOrElse(1) { "false" }.toBoolean(), notifier))
            notifier.launchAsCoroutine {
                profileManager.downloadGame(notifier) && modManager.downloadMods(profile, notifier)
                notifier.success("Update successful, server version is now **${profile.gameVersion.versionNumber}**").queue()
            }
    }

    fun runUpdateModCommand(notifier: Notifier, args: List<String>) {
        val profile = profileManager.currentProfileOrThrow
        val modName = args.firstOrNull() ?: throw MissingArgumentException("update mod", "name")
        val version = args.getOrNull(1)

        modManager.removeMod(notifier, modName, true)
        if (version == null) // FIXME Only one call with default argument
            modManager.addMod(notifier, modName)
        else
            modManager.addMod(notifier, modName, version)
        modManager.downloadMods(profile, notifier)
        notifier.success("Successfully updated **$modName** to **${version ?: "latest version"}**").queue()
    }

    fun runUpdateAllCommand(notifier: Notifier, args: List<String>) {
        val profile = profileManager.currentProfileOrThrow
        profileManager.updateProfile(profile, notifier = notifier)
        modManager.getModReleaseListByProfile(profile).forEach {
            modManager.apply {
                removeMod(notifier, it.mod.name, true)
                addMod(notifier, it.mod.name)
            }
        }
        notifier.launchAsCoroutine {
            profileManager.downloadGame(notifier) && modManager.downloadMods(profile, notifier)
            notifier.success("Update successful, everything is now to latest version").queue()
        }
    }

    fun runSwapCommand(notifier: Notifier, args: List<String>) {
        val name = args.firstOrNull() ?: throw MissingArgumentException("swap", "name")

        profileManager.swapProfile(name)
        notifier.success("Current profile is now **$name**").queue()
    }

    fun runStopCommand(notifier: Notifier, args: List<String>) {
        if (processManager.stop())
            notifier.success("Server stopped").queue()
        else
            notifier.error("Error: No process is currently running")
    }

    fun runBuildCommand(notifier: Notifier, args: List<String>) {
        val profile = profileManager.currentProfileOrThrow

        notifier.launchAsCoroutine {
            notifier.running("Starting build...").queue()
            profileManager.downloadGame(notifier) && modManager.downloadMods(profile, notifier) && processManager.genMap(profile, notifier)
        }
    }

    fun runCreateProfileCommand(notifier: Notifier, args: List<String>) {
        val name = args.firstOrNull() ?: throw MissingArgumentException("create profile", "name")
        val targetVersion = args.getOrNull(1)
        val experimental = args.getOrNull(2) == "experimental"

        profileManager.createProfile(name, targetVersion, experimental, notifier)
    }

    fun runCopyProfileCommand(notifier: Notifier, args: List<String>) {
        val name = args.firstOrNull() ?: throw MissingArgumentException("copy profile", "new_name")
        val profile = profileManager.currentProfileOrThrow

        profileManager.copyProfile(name, profile, notifier)
    }

    fun runRemoveModCommand(notifier: Notifier, args: List<String>) {
        val modName = args.firstOrNull() ?: throw MissingArgumentException("remove mod", "name")
        notifier.running("Trying to remove **$modName**...").queue()
        modManager.removeMod(notifier, modName)
    }

    fun runRemoveFileCommand(notifier: Notifier, args: List<String>) {
        val fileName = args.firstOrNull() ?: throw MissingArgumentException("remove file", "name")
        profileManager.removeFile(notifier, fileName)
    }

    fun runAddModCommand(notifier: Notifier, args: List<String>) {
        val modName = args.firstOrNull() ?: throw MissingArgumentException("add mod", "name")
        val version = args.getOrNull(1)
        profileManager.currentProfileOrThrow
        notifier.running("Trying to add **$modName**...").queue()
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
        notifier.success("Successfully revoked token for **${profile.name}**, all related links are now invalid")
                .queue()
    }

    fun runGenerateStellarisBuildsCommand(notifier: Notifier, args: List<String>) {
        stellarisManager.generateHumanBuilds(notifier.event.message, notifier)
    }

    fun runTestCommand(notifier: Notifier, args: List<String>) {
        notifier.launchAsCoroutine {
            notifier.running("Wait for it.").flush()
            delay(3_000L)
            notifier.running("Wait for it..").flush()
            delay(3_000L)
            notifier.running("Wait for it...").flush()
            delay(3_000L)
            notifier.success("**It works!** :partying_face:").queue()
        }
    }

    fun runHelpCommand(notifier: Notifier, args: List<String>) {
        val msg = if (args.isEmpty()) {
            listOfAvailableCommands
        } else {
            ":small_blue_diamond: " + (getCommand(args.iterator())?.help ?: "Comand ${args.reduce { acc, s -> "$acc $s" }} not found")
        }
        notifier.message(msg).flush()
    }

    companion object {
        fun notImplemented(notifier: Notifier) =
                notifier.error("Feature not implemented yet : ${notifier.event.message.contentDisplay}")
    }

    fun removePrefix(str: String) =
            if (str.startsWith(config.discord.commandPrefix)) str.drop(config.discord.commandPrefix.length) else null

    fun parseCommand(event: MessageReceivedEvent) {
        logger.info("Parsing command: ${event.message.contentRaw}")
        val content = event.message.contentRaw.trimStart()
        val str = removePrefix(content) ?: if (event.isFromType(ChannelType.PRIVATE)) content else return
        val tokens = str.trimStart().split(' ').filter { it.isNotEmpty() }
        val notifier = Notifier(event)
        val cmd = getCommand(tokens.iterator()) ?: return notifier.parseError()

        try {
            cmd(this, notifier, tokens.drop(cmd.depthLevel))
        } catch (e: Exception) {
            notifier.error(e)
        }
    }
}
