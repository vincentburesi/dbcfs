package fr.ct402.dbcfs.factorio.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fr.ct402.dbcfs.commons.AbstractComponent
import fr.ct402.dbcfs.discord.Notifier
import fr.ct402.dbcfs.factorio.FactorioConfigProperties
import fr.ct402.dbcfs.persist.DbLoader
import fr.ct402.dbcfs.persist.model.Mod
import fr.ct402.dbcfs.persist.model.Mods
import khttp.get
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
    val jsonMapper = jacksonObjectMapper()
    fun factorioModPortalUrl(page: Int, pageSize: Int = 25) = "http://mods.factorio.com/api/mods" +
            "?username=${config.username}&token=${config.token}&page_size=$pageSize${if (page > 1) "&page=$page" else ""}"

    @JsonIgnoreProperties("links")
    data class Pagination(val count: Int, val page: Int, val page_count: Int, val page_size: Int)

    data class Result(
            val name: String, val title: String, val owner: String, val summary: String,
            val downloads_count: Int, val category: String?, val score: Float, val latest_release: LatestRelease?
    )

    data class LatestRelease(
            val download_url: String, val file_name: String, val info_json: InfoJson, val released_at: String,
            val version: String, val sha1: String
    )

    data class InfoJson(val factorio_version: String)
    data class ModList(val pagination: Pagination, val results: List<Result>)

    fun retrieveModList(notifier: Notifier): MutableList<Result>? {
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
            notifier.error("An error occured during mod retrieval")
            return null
        }
    }

    private fun List<Result>.sanitize(): List<Result> = this.filter { it.latest_release != null }

    fun syncModList(notifier: Notifier): Boolean {
        notifier.update("Starting mod versions sync...", force = true)
        val modList = retrieveModList(notifier)?.sanitize() ?: return false
        val existingMods = modSequence().toList()
        notifier.update("${modList.size} mods retrieved, updating DB (this might take some time)...")
        updateDb(modList, existingMods, notifier)
        notifier.success("Successfully synced mod versions")
        return true
    }

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
}

