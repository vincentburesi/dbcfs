package fr.ct402.dbcfs.persist.model

import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.Table
import me.liuwj.ktorm.schema.int

interface ModReleaseProfileMapping : Entity<ModReleaseProfileMapping> {
    companion object : Entity.Factory<ModReleaseProfileMapping>()

    var id: Int
    var modRelease: ModRelease
    var profile: Profile
}

object ModReleaseProfileMappings : Table<ModReleaseProfileMapping>("t_mod_release_profile_mapping") {
    val id = int("rowid").primaryKey().bindTo { it.id }
    val modRelease = int("mod_release_id").references(ModReleases) { it.modRelease }
    val profile = int("profile_id").references(Profiles) { it.profile }
}

val modReleaseProfileMappingSchema = """
    CREATE TABLE IF NOT EXISTS t_mod_release_profile_mapping (
        mod_release_id INTEGER NOT NULL,
        profile_id INTEGER NOT NULL
    )
""".trimIndent()