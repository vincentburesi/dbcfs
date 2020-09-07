package fr.ct402.dbcfs.refactor.factorio.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class ModList(
        val pagination: Pagination,
        val results: List<Result>,
)

@JsonIgnoreProperties("links")
data class Pagination(
        val count: Int,
        val page: Int,
        val page_count: Int,
        val page_size: Int,
)

data class Result(
        val name: String,
        val title: String,
        val owner: String,
        val summary: String,
        val downloads_count: Int,
        val category: String?,
        val score: Float,
        val latest_release: ModDetailRelease?,
)
data class InfoJson(
        val factorio_version: String,
        val dependencies: List<String>?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ModDetail(
        val releases: List<ModDetailRelease>,
)

data class ModDetailRelease(
        val download_url: String,
        val file_name: String,
        val info_json: InfoJson,
        val released_at: String,
        val version: String,
        val sha1: String,
)
