package fr.ct402.dbcfs.persist.model

import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.*

interface Mod: Entity<Mod> {
    companion object : Entity.Factory<Mod>()

    val id: Int
    var name: String
    var title: String
    var owner: String
    var summary: String
    var downloadsCount: Int
    var category: String?
    var score: Float
    var latestReleaseDownloadUrl: String
}

object Mods : Table<Mod>("t_mod") {
    val id = int("rowid").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
    val title = varchar("title").bindTo { it.title }
    val owner = varchar("owner").bindTo { it.owner }
    val summary = varchar("summary").bindTo { it.summary }
    val downloadsCount = int("downloads_count").bindTo { it.downloadsCount }
    val category = varchar("category").bindTo { it.category }
    val score = float("score").bindTo { it.score }
    val latestReleaseDownloadUrl = varchar("latest_release_download_url").bindTo { it.latestReleaseDownloadUrl }
}


val modSchema = """
    CREATE TABLE IF NOT EXISTS t_mod (
        name TEXT NOT NULL UNIQUE,
        title TEXT NOT NULL,
        owner TEXT NOT NULL,
        summary TEXT NOT NULL,
        downloads_count INTEGER NOT NULL,
        category TEXT,
        score REAL NOT NULL,
        latest_release_download_url TEXT NOT NULL
    )
""".trimIndent()
