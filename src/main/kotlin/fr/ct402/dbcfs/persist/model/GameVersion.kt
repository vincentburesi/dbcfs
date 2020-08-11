package fr.ct402.dbcfs.persist.model

import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.*

enum class ReleaseType { ALPHA, HEADLESS }
enum class Platform { WIN64, WIN32, WIN64_MANUAL, WIN32_MANUAL, OSX, LINUX64, LINUX32 }

interface GameVersion : Entity<GameVersion> {
    companion object : Entity.Factory<GameVersion>()

    val id: Int
    var versionNumber: String
    var releaseType: ReleaseType
    var platform: Platform
    var isStable: Boolean
    var path: String
}

object GameVersions : Table<GameVersion>("t_game_version") {
    val id = int("rowid").primaryKey().bindTo { it.id }
    val versionNumber = varchar("version_number").bindTo { it.versionNumber }
    val releaseType = enum("release_type", typeRef<ReleaseType>()).bindTo { it.releaseType }
    val platform = enum("platform", typeRef<Platform>()).bindTo { it.platform }
    val isStable = boolean("stable").bindTo { it.isStable }
    val path = varchar("path").bindTo { it.path }
}

val gameVersionSchema = """
    CREATE TABLE IF NOT EXISTS t_game_version (
        version_number TEXT NOT NULL,
        release_type TEXT NOT NULL,
        platform TEXT NOT NULL,
        stable BOOLEAN NOT NULL,
        path TEXT NOT NULL UNIQUE
    )
""".trimIndent()
