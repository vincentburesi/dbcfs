package fr.ct402.dbcfs.persist.model

import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.*

interface GameVersion : Entity<GameVersion> {
    enum class BuildType { ALPHA, HEADLESS }
    enum class Platform { WIN64, WIN32, WIN64_MANUAL, WIN32_MANUAL, OSX, LINUX64, LINUX32 }

    companion object : Entity.Factory<GameVersion>()

    var id: Int
    var versionNumber: String
    var buildType: BuildType
    var platform: Platform
    var isStable: Boolean
    var path: String
    var localPath: String?
}

object GameVersions : Table<GameVersion>("t_game_version") {
    val id = int("rowid").primaryKey().bindTo { it.id }
    val versionNumber = varchar("version_number").bindTo { it.versionNumber }
    val buildType = enum("build_type", typeRef<GameVersion.BuildType>()).bindTo { it.buildType }
    val platform = enum("platform", typeRef<GameVersion.Platform>()).bindTo { it.platform }
    val isStable = boolean("stable").bindTo { it.isStable }
    val path = varchar("path").bindTo { it.path }
    val localPath = varchar("local_path").bindTo { it.localPath }
}

val gameVersionSchema = """
    CREATE TABLE IF NOT EXISTS t_game_version (
        version_number TEXT NOT NULL,
        build_type TEXT NOT NULL,
        platform TEXT NOT NULL,
        stable BOOLEAN NOT NULL,
        path TEXT NOT NULL UNIQUE,
        local_path TEXT UNIQUE
    )
""".trimIndent()
