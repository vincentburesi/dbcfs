package fr.ct402.dbcfs.manager

import fr.ct402.dbcfs.commons.ModNotFoundException
import fr.ct402.dbcfs.commons.NoCurrentProfileException
import fr.ct402.dbcfs.commons.getLogger
import fr.ct402.dbcfs.persist.DbLoader
import fr.ct402.dbcfs.persist.model.*
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.*
import org.springframework.stereotype.Component

@Component
class ModManager(
        val profileManager: ProfileManager,
        val dbLoader: DbLoader,
) {
    val logger = getLogger()
    fun modReleaseProfileSequence() = dbLoader.database.sequenceOf(ModReleaseProfileMappings, withReferences = true)
    fun modReleaseSequence() = dbLoader.database.sequenceOf(ModReleases)
    fun modSequence() = dbLoader.database.sequenceOf(Mods)

    fun getModByNameOrThrow(modName: String) =
            getModByName(modName) ?: throw ModNotFoundException(modName)

    fun getModByName(modName: String): Mod? {
        return modSequence().find { it.name eq modName }
    }

    fun addMod(modRelease: ModRelease) {
        val profile = profileManager.currentProfileOrThrow
        // - ModRelease entity exists
        // - ModVersion was already picked
        // - ModVersion picked and transfered
        // Check for duplicates
        val releasesIds = modReleaseSequence().filter { it.mod eq modRelease.mod.id }.toList().map { it.id }
        val duplicate = modReleaseProfileSequence().find { (it.profile eq profile.id) and it.modRelease.inList(releasesIds) }
        if (duplicate != null) {
            logger.error("TMP duplicate found")
            return //FIXME throw proper exception
        }

        // Add mods
        val modReleaseProfileMapping = ModReleaseProfileMapping().apply {
            this.modRelease = modRelease
            this.profile = profile
        }
        modReleaseProfileSequence().add(modReleaseProfileMapping)

        // - Check profile for inconsistencies
    }

    fun addMod(modName: String, exactVersion: String) {
        val mod = modSequence().find { it.name eq modName }
        if (mod == null) {
            logger.error("TMP mod not found")
            return
        }
        val modRelease = modReleaseSequence().find { it.version eq exactVersion }
        if (modRelease == null) {
            logger.error("TMP mod RELEASE not found (add it from API)")
            return
        }
        addMod(modRelease)
    }

    fun addMod(modName: String) {
        val mod = modSequence().find { it.name eq modName }
        if (mod == null) {
            logger.error("TMP mod not found")
            return
        }
        // TODO Smarter mod selection
        logger.error("Searching for release ${mod.latestReleaseDownloadUrl}...")
        val modRelease = modReleaseSequence().find { it.downloadUrl eq mod.latestReleaseDownloadUrl }
        if (modRelease == null) {
            logger.error("TMP mod RELEASE not found (add it from API)")
            modReleaseSequence().toList().forEach {
                logger.error(it.downloadUrl)
            }
            return
        }
        logger.error("found it !")
        addMod(modRelease)
    }

    fun removeMod(modName: String) {
        val profile = profileManager.currentProfileOrThrow
        val mod = modSequence().find { it.name eq modName }
        if (mod == null) {
            logger.error("TMP mod not found")
            return
        }

        val result = modReleaseProfileSequence().database
                .from(ModReleaseProfileMappings)
                .leftJoin(ModReleases, on = ModReleaseProfileMappings.modRelease eq ModReleases.id)
                .leftJoin(Mods, on = ModReleases.mod eq Mods.id)
                .select(ModReleaseProfileMappings.columns)
                .where { (Mods.name eq mod.name) and (ModReleaseProfileMappings.profile eq profile.id) }
                .map { ModReleaseProfileMappings.createEntity(it) }
        result.forEach { entry ->
            logger.info("Removing ${entry.id}|${entry.modRelease.id}|${entry.profile.id}")
            modReleaseProfileSequence().removeIf { it.id eq entry.id }
        }
    }
}