package fr.ct402.dbcfs.persist.model

import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.Table
import me.liuwj.ktorm.schema.int
import me.liuwj.ktorm.schema.varchar

interface ModRelease: Entity<ModRelease> {
    companion object: Entity.Factory<ModRelease>()

    var id: Int
    var downloadUrl: String
    var fileName: String
    var infoJson: String // TODO Dependency list
    var factorioVersion: String
    var releasedAt: String
    var version: String
    var sha1: String
    var mod: Mod
}

object ModReleases : Table<ModRelease>("t_mod_release") {
    val id = int("rowid").primaryKey().bindTo { it.id }
    val downloadUrl = varchar("download_url").bindTo { it.downloadUrl }
    val fileName = varchar("file_name").bindTo { it.fileName }
    val infoJson = varchar("info_json").bindTo { it.infoJson }
    val factorioVersion = varchar("factorio_version").bindTo { it.factorioVersion }
    val releasedAt = varchar("released_at").bindTo { it.releasedAt }
    val version = varchar("version").bindTo { it.version }
    val sha1 = varchar("sha1").bindTo { it.sha1 }
    val mod = int("mod_id").references(Mods) { it.mod }
}

val modReleaseSchema = """
    CREATE TABLE IF NOT EXISTS t_mod_release (
        download_url TEXT NOT NULL UNIQUE,
        file_name TEXT NOT NULL UNIQUE,
        info_json TEXT NOT NULL,
        factorio_version TEXT NOT NULL,
        released_at TEXT NOT NULL,
        version TEXT NOT NULL,
        sha1 TEXT NOT NULL,
        mod_id INTEGER NOT NULL
    )
""".trimIndent()
