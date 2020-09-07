package fr.ct402.dbcfs.refactor.persist.model

import fr.ct402.dbcfs.refactor.commons.parseDateTime
import fr.ct402.dbcfs.refactor.commons.printDateTime
import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.*
import java.time.LocalDateTime

interface Profile : Entity<Profile> {
    companion object : Entity.Factory<Profile>()

    var id: Int
    var name: String
    var targetGameVersion: String
    var allowExperimental: Boolean
    var gameVersion: GameVersion
    var token: String?
    var tokenExpirationDateString: String?

    var tokenExpiration: LocalDateTime?
            get() {
                return parseDateTime(tokenExpirationDateString ?: return null)
            }
            set(dateTime) {
                tokenExpirationDateString =
                        if (dateTime == null)
                            null
                        else
                            printDateTime(dateTime)
            }
    val localPath: String
            get() = "/mnt/profiles/$name"

    fun checkAuth(authToken: String) =
            !token.isNullOrBlank() && token == authToken && LocalDateTime.now().isBefore(tokenExpiration)
    fun invalidateToken() = apply { token = null }.flushChanges()
}

object Profiles : Table<Profile>("t_profile") {
    val id = int("rowid").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
    val targetGameVersion = varchar("target_game_version").bindTo { it.targetGameVersion }
    val allowExperimental = boolean("allow_experimental").bindTo { it.allowExperimental }
    val gameVersion = int("game_version_id").references(GameVersions) { it.gameVersion }
    val token = varchar("token").bindTo { it.token }
    val tokenExpirationDateString = varchar("token_expiration_date_string").bindTo { it.tokenExpirationDateString }
}

val profileSchema = """
    CREATE TABLE IF NOT EXISTS t_profile (
        name TEXT NOT NULL UNIQUE,
        target_game_version TEXT NOT NULL,
        allow_experimental BOOLEAN NOT NULL,
        game_version_id INTEGER NOT NULL,
        token TEXT,
        token_expiration_date_string TEXT
    )
""".trimIndent()
