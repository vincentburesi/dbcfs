package fr.ct402.dbcfs.manager

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fr.ct402.dbcfs.commons.FactorioApiErrorException
import fr.ct402.dbcfs.commons.ModNotFoundException
import fr.ct402.dbcfs.commons.ModReleaseNotFoundException
import fr.ct402.dbcfs.commons.getLogger
import fr.ct402.dbcfs.discord.Notifier
import fr.ct402.dbcfs.factorio.FactorioConfigProperties
import fr.ct402.dbcfs.factorio.api.ModPortalApiService
import fr.ct402.dbcfs.persist.DbLoader
import fr.ct402.dbcfs.persist.model.*
import khttp.get
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.*
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.io.File

@Component
@Configuration
class ModManager(
        private val config: FactorioConfigProperties,
        val profileManager: ProfileManager,
        val modPortalApiService: ModPortalApiService,
        val dbLoader: DbLoader,
) {
    val logger = getLogger()
    private val jsonMapper = jacksonObjectMapper()
    fun modReleaseProfileMappingsSequence() = dbLoader.database.sequenceOf(ModReleaseProfileMappings, withReferences = true)
    fun modReleaseSequence() = dbLoader.database.sequenceOf(ModReleases)
    fun modSequence() = dbLoader.database.sequenceOf(Mods)

    fun getModByName(modName: String): Mod? = modSequence().find { it.name eq modName }
    fun getModByNameOrThrow(modName: String) = getModByName(modName) ?: throw ModNotFoundException(modName)
    fun getModReleaseByVersion(mod: Mod, version: String) = modReleaseSequence().find { (it.mod eq mod.id) and (it.version eq version) }
    fun getModReleaseByVersionOrThrow(mod: Mod, version: String) = getModReleaseByVersion(mod, version) ?: throw ModReleaseNotFoundException(mod.name, version)
    fun getLatestModRelease(mod: Mod) = modReleaseSequence().find { it.downloadUrl eq mod.latestReleaseDownloadUrl }
    fun getLatestModReleaseOrThrow(mod: Mod) = getLatestModRelease(mod) ?: throw ModReleaseNotFoundException(mod.name)

    fun addMod(notifier: Notifier, modRelease: ModRelease) {
        val profile = profileManager.currentProfileOrThrow
        val releasesIds = modReleaseSequence().filter { it.mod eq modRelease.mod.id }.toList().map { it.id }
        val duplicate = modReleaseProfileMappingsSequence().find { (it.profile eq profile.id) and it.modRelease.inList(releasesIds) }
        if (duplicate != null) {
            notifier.error("Did not add mod ${modRelease.mod.name}, mod already present. Remove it first to change version")
            return
        }

        val modReleaseProfileMapping = ModReleaseProfileMapping().apply {
            this.modRelease = modRelease
            this.profile = profile
        }
        modReleaseProfileMappingsSequence().add(modReleaseProfileMapping)
        notifier.success("Successfully added ${modRelease.mod.name} to ${profile.name}")
    }

    fun addMod(notifier: Notifier, modName: String, exactVersion: String) {
        val mod = getModByNameOrThrow(modName)
        val modRelease = getModReleaseByVersion(mod, exactVersion).run {
            if (this == null) {
                modPortalApiService.syncModReleaseList(mod, notifier)
                getModReleaseByVersionOrThrow(mod, exactVersion)
            } else this
        }
        notifier.update("Found ${mod.name} version ${modRelease.version}...")
        addMod(notifier, modRelease)
    }

    fun addMod(notifier: Notifier, modName: String) {
        val mod = getModByNameOrThrow(modName)
        val modRelease = getLatestModRelease(mod).run {
            if (this == null) {
                modPortalApiService.syncModReleaseList(mod, notifier)
                getLatestModReleaseOrThrow(mod)
            } else this
        }
        notifier.update("Found ${mod.name} version ${modRelease.version}...")
        addMod(notifier, modRelease)
    }

    fun removeMod(notifier: Notifier, modName: String) {
        val profile = profileManager.currentProfileOrThrow
        val mod = getModByNameOrThrow(modName)

        val result = modReleaseProfileMappingsSequence().database
                .from(ModReleaseProfileMappings)
                .leftJoin(ModReleases, on = ModReleaseProfileMappings.modRelease eq ModReleases.id)
                .leftJoin(Mods, on = ModReleases.mod eq Mods.id)
                .select(ModReleaseProfileMappings.columns)
                .where { (Mods.name eq mod.name) and (ModReleaseProfileMappings.profile eq profile.id) }
                .map { ModReleaseProfileMappings.createEntity(it) }
        if (result.isEmpty())
            notifier.error("Did not find ${mod.name} in ${profile.name} list of mods")
        else {
            result.forEach { entry ->
                val modRelease = modReleaseSequence().find { it.id eq entry.modRelease.id }
                if (modRelease == null) {
                    notifier.error("Did not find ${mod.name} in ${profile.name} list of mods")
                    return
                }

                File("${profile.localPath}/mods/${modRelease.fileName}")
                        .apply {
                            logger.error("Removing ${this.absolutePath}")
                            if (exists()) delete()
                        }
                File("${profile.localPath}/mods/mod-list.json")
                        .apply { if (exists()) delete() }
                modReleaseProfileMappingsSequence().removeIf { it.id eq entry.id }
            }
            notifier.success("Successfully removed ${mod.name} from ${profile.name} list of mods")
        }
    }

    fun removeAllModsFromProfile(profile: Profile, notifier: Notifier){
        notifier.update("Removing profile related mods...", force = true)
        modReleaseProfileMappingsSequence().removeIf { it.profile eq profile.id }
    }

    data class JsonModElem(val name: String, val enabled: Boolean)
    data class JsonModList(val mods: List<JsonModElem>)

    fun modDownloadUrl(modRelease: ModRelease) =
            "https://mods.factorio.com/${modRelease.downloadUrl}?username=${config.username}&token=${config.token}"

    fun downloadMods(profile: Profile, notifier: Notifier): Boolean {
        val modList = getModReleaseListByProfile(profile)
        if (modList.isEmpty())
            return true
        notifier.update("Retrieving mods...")

        modList.forEachIndexed { index, modRelease ->
            notifier.update("Retrieving mods ($index of ${modList.size})...")
            val res = get(modDownloadUrl(modRelease), stream = true)
            if (res.statusCode != 200) throw FactorioApiErrorException()

            val file = File("${profile.localPath}/mods/${modRelease.fileName}")
            if (!file.exists())
                for (chunk in res.contentIterator(1024))
                    file.appendBytes(chunk)
        }

        val jsonModList = JsonModList(modList.map { JsonModElem(it.mod.name, true) })
        File("${profile.localPath}/mods/mod-list.json")
                .writeText(jsonMapper.writeValueAsString(jsonModList))

        notifier.success("Successfully downloaded mods")
        return true
    }

    fun getModReleaseListByProfile(profile: Profile): List<ModRelease> = modReleaseProfileMappingsSequence()
            .filter { it.profile eq profile.id }
            .toList()
            .map { it.modRelease }

    fun getModReleaseListByName(modName: String): List<ModRelease>{
        val mod = getModByNameOrThrow(modName)
        return modReleaseSequence().filter { it.mod eq mod.id }.toList()
    }
}