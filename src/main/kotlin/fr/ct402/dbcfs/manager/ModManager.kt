package fr.ct402.dbcfs.manager

import fr.ct402.dbcfs.commons.ModNotFoundException
import fr.ct402.dbcfs.commons.NoCurrentProfileException
import fr.ct402.dbcfs.commons.getLogger
import fr.ct402.dbcfs.persist.DbLoader
import fr.ct402.dbcfs.persist.model.*
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.dsl.inList
import me.liuwj.ktorm.entity.*
import org.springframework.stereotype.Component

@Component
class ModManager(
        val profileManager: ProfileManager,
        dbLoader: DbLoader,
) {
    val logger = getLogger()
    val modReleaseProfileSequence = dbLoader.database.sequenceOf(ModReleaseProfileMappings, withReferences = true)
    val modReleaseSequence = dbLoader.database.sequenceOf(ModReleases)
    val modSequence = dbLoader.database.sequenceOf(Mods)

    fun getModByNameOrThrow(modName: String) =
            getModByName(modName) ?: throw ModNotFoundException(modName)

    fun getModByName(modName: String): Mod? {
        return modSequence.find { it.name eq modName }
    }

    fun addMod(modRelease: ModRelease) {
        val profile = profileManager.currentProfileOrThrow
        // - ModRelease entity exists
        // - ModVersion was already picked
        // - ModVersion picked and transfered
        // Check for duplicates
        val releasesIds = modReleaseSequence.filter { it.mod eq modRelease.mod.id }.toList().map { it.id }
        val duplicate = modReleaseProfileSequence.find { it.modRelease.inList(releasesIds) }
        if (duplicate != null) {
            logger.error("TMP duplicate found")
            return //FIXME throw proper exception
        }

        // Add mods
        val modReleaseProfileMapping = ModReleaseProfileMapping().apply {
            this.modRelease = modRelease
            this.profile = profile
        }
        modReleaseProfileSequence.add(modReleaseProfileMapping)

        // - Check profile for inconsistencies
    }

    fun addMod(modName: String, exactVersion: String) {
        // - Mod entity exists
        // - ModRelease chosen
        // - ModRelease picked and transfered
        val mod = modSequence.find { it.name eq modName }
        if (mod == null) {
            logger.error("TMP mod not found")
            return
        }
        val modRelease = modReleaseSequence.find { it.version eq exactVersion }
        if (modRelease == null) {
            logger.error("TMP mod RELEASE not found (add it from API)")
            return
        }
        addMod(modRelease)
    }

}