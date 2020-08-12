package fr.ct402.dbcfs.factorio.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fr.ct402.dbcfs.commons.getLogger
import fr.ct402.dbcfs.factorio.FactorioConfigProperties
import fr.ct402.dbcfs.persist.DbLoader
import khttp.get
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
@Configuration
class ModPortalApiService(
        private val config: FactorioConfigProperties,
        dbLoader: DbLoader
) {
    val logger = getLogger()

    @JsonIgnoreProperties("links")
    data class Pagination(val count: Int, val page: Int, val page_count: Int, val page_size: Int)
    data class Result(
            val name: String, val title: String, val owner: String, val summary: String,
            val downloads_count: Int, val category: String?, val score: Float, val latest_release: LatestRelease
    )
    data class LatestRelease(
            val download_url: String, val file_name: String, val info_json: InfoJson, val released_at: String,
            val version: String, val sha1: String
    )
    data class InfoJson(val factorio_version: String)
    data class ModList(val pagination: Pagination, val results: List<Result>)

    //    val gameVersionSequence = dbLoader.database.sequenceOf(GameVersions)
    val jsonMapper = jacksonObjectMapper()
    val factorioModPortalUrl = "http://mods.factorio.com/api/mods?page_size=6481"

//    @EventListener(ApplicationReadyEvent::class)
    fun syncModList() {
        logger.info("Starting mod list sync")
        val res = get(factorioModPortalUrl)

        if (res.statusCode != 200) {
            logger.error("Failed to recover mod list, error code : ${res.statusCode}")
            return
        }

        logger.info(res.text)
        val modList = jsonMapper.readValue<ModList>(res.text)
//        modList.results.forEach { logger.info(it.latest_release.info_json.toString()) }
        modList.results.map { it.category }.distinct().forEach { logger.info(it) }

        // TODO Store list of mods in DB
    }

    fun downloadMod() {

    }
}

