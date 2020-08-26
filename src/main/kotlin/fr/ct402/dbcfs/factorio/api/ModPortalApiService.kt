package fr.ct402.dbcfs.factorio.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fr.ct402.dbcfs.commons.AbstractComponent
import fr.ct402.dbcfs.commons.FactorioApiErrorException
import fr.ct402.dbcfs.discord.Notifier
import fr.ct402.dbcfs.factorio.FactorioConfigProperties
import fr.ct402.dbcfs.persist.DbLoader
import fr.ct402.dbcfs.persist.model.Mod
import fr.ct402.dbcfs.persist.model.ModRelease
import fr.ct402.dbcfs.persist.model.ModReleases
import fr.ct402.dbcfs.persist.model.Mods
import khttp.get
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.entity.*
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import java.lang.Exception
import kotlin.math.abs

@Service
@Configuration
class ModPortalApiService(
        private val config: FactorioConfigProperties,
        val dbLoader: DbLoader
): AbstractComponent() {
    fun modSequence() = dbLoader.database.sequenceOf(Mods)
    fun modReleaseSequence() = dbLoader.database.sequenceOf(ModReleases)
    val jsonMapper = jacksonObjectMapper()
    fun factorioModPortalUrl(page: Int, pageSize: Int = 25) = "http://mods.factorio.com/api/mods" +
            "?username=${config.username}&token=${config.token}&page_size=$pageSize${if (page > 1) "&page=$page" else ""}"

    fun factorioModDetailUrl(modName: String) = "http://mods.factorio.com/api/mods/$modName/full"

    fun syncModList(notifier: Notifier): Boolean {
        notifier.update("Starting mod versions sync...", force = true)
        val modList = retrieveModList(notifier)?.sanitize() ?: return false
        val existingMods = modSequence().toList()
        notifier.update("${modList.size} mods retrieved, updating DB (this might take some time)...")
        updateDb(modList, existingMods, notifier)
        notifier.success("Successfully synced mod versions")
        return true
    }

    fun syncModReleaseList(notifier: Notifier, mod: Mod) {
        val releases = retrieveModReleaseList(mod)
        val existings = modReleaseSequence().filter { it.mod eq mod.id }.toList()

        updateModReleasesDb(mod, releases, existings, notifier)
        notifier.success("Successfully updated mod release list")
    }

    //region syncModList internals
    private fun retrieveModList(notifier: Notifier): MutableList<Result>? {
        val pageSize = 200
        try {
            var i = 0
            var count = 1
            var acc: MutableList<Result> = arrayListOf()
            while (++i <= count) {
                notifier.update("Obtaining page $i of $count (page size: $pageSize, starting at ${(i - 1) * pageSize})...")
                val res = get(factorioModPortalUrl(i, pageSize))
                if (res.statusCode != 200) {
                    logger.error("Error: Mod API returned error status code for : ${factorioModPortalUrl(i, pageSize)}")
                    continue //TODO Notify user with a warning
                }
                val page = jsonMapper.readValue<ModList>(res.text)
                count = page.pagination.page_count
                acc.addAll(page.results)
            }
            return acc
        } catch (e: Exception) {
            logger.error("Error during modList retrieval : ${e.message}")
            throw FactorioApiErrorException()
        }
    }

    private fun List<Result>.sanitize(): List<Result> = this.filter { it.latest_release != null }

    private fun updateDb(modList: List<Result>, existingMods: List<Mod>, notifier: Notifier) =
            modList.forEachIndexed { index, mod ->
                val existing = existingMods.find { it.name == mod.name }
                buildDbEntry(mod, existing)?.updateOrAdd(existing == null)
                if (index % 20 == 0)
                    notifier.update("${modList.size} mods retrieved, updating DB (this might take some time)..." +
                            " ${ (index * 100) / modList.size }%")
            }

    // Only updates the DB when the mod has changed in a significant way
    private fun Result.hasSignificantChange(old: Mod?) = (old == null
            || old.name != name
            || old.title != title
            || old.owner != owner
            || old.summary != summary
            || abs(old.downloadsCount - downloads_count) > 100
            || old.category != category
            || abs(old.score - score) > 1.0F
            || old.latestReleaseDownloadUrl != latest_release?.download_url)

    private fun buildDbEntry(mod: Result, existing: Mod? = null): Mod? {
        return (existing ?: Mod()).apply {
            if (mod.hasSignificantChange(this)) {
                name = mod.name
                title = mod.title
                owner = mod.owner
                summary = mod.summary
                downloadsCount = mod.downloads_count
                category = mod.category
                score = mod.score
                latestReleaseDownloadUrl = mod.latest_release!!.download_url
            } else {
                logger.debug("Mod ${mod.name} unchanged, skipped.")
                return null
            }
        }
    }

    private fun Mod.updateOrAdd(add: Boolean) =
            if (add) modSequence().add(this) else this.flushChanges()
    //endregion

    //region syncModReleaseList internals
    private fun retrieveModReleaseList(mod: Mod): List<ModDetailRelease> {
        val res = get(factorioModDetailUrl(mod.name))
        if (res.statusCode != 200) {
            logger.error("Error: Mod Detail API returned error status code for : ${factorioModDetailUrl(mod.name)}")
            throw FactorioApiErrorException()
        }
        return jsonMapper.readValue<ModDetail>(res.text).releases
    }

    private fun updateModReleasesDb(mod: Mod, releases: List<ModDetailRelease>, existingReleases: List<ModRelease>, notifier: Notifier) =
            releases.forEachIndexed { index, release ->
                notifier.update("Adding modRelease $index of ${releases.size}...")
                val existing = existingReleases.find { it.downloadUrl == release.download_url }
                buildDbEntry(mod, release, existing)?.updateOrAdd(existing == null)
            }

    private fun buildDbEntry(mod: Mod, release: ModDetailRelease, existing: ModRelease? = null): ModRelease? {
        return (existing ?: ModRelease()).apply {
            if (release.differs(this)) {
                downloadUrl = release.download_url
                fileName = release.file_name
                infoJson = release.info_json.toString()
                releasedAt = release.released_at
                version = release.version
                sha1 = release.sha1
                this.mod = mod
            } else {
                logger.debug("Mod ${release.version} unchanged, skipped.")
                return null
            }
        }
    }

    private fun ModDetailRelease.differs(old: ModRelease?) = (old == null
            || old.fileName != file_name
            || old.infoJson != info_json.toString()
            || old.releasedAt != released_at
            || old.version != version
            || old.sha1 != sha1)

    private fun ModRelease.updateOrAdd(add: Boolean) =
            if (add) modReleaseSequence().add(this) else this.flushChanges()
    //endregion
}

