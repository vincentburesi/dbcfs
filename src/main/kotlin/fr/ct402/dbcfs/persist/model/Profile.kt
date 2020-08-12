package fr.ct402.dbcfs.persist.model

import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.*

interface Profile : Entity<Profile> {
    companion object : Entity.Factory<Profile>()

    val id: Int
    var name: String
    var targetGameVersion: String
    var allowExperimental: Boolean
    var gameVersion: GameVersion
}

object Profiles : Table<Profile>("t_profile") {
    val id = int("rowid").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
    val targetGameVersion = varchar("target_game_version").bindTo { it.targetGameVersion }
    val allowExperimental = boolean("allow_experimental").bindTo { it.allowExperimental }
    val gameVersion = int("game_version_id").references(GameVersions) { it.gameVersion }
}

val profileSchema = """
    CREATE TABLE IF NOT EXISTS t_game_version (
        name TEXT NOT NULL UNIQUE,
        target_game_version TEXT NOT NULL,
        allow_experimental BOOLEAN NOT NULL,
        game_version_id INTEGER NOT NULL
    )
""".trimIndent()
