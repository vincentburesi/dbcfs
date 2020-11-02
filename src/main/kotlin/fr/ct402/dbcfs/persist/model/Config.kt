package fr.ct402.dbcfs.persist.model

import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.*

interface Config : Entity<Config> {
    companion object : Entity.Factory<Config>()

    var id: Int
    var factorioUsername: String
    var factorioToken: String
    var factorioSessionCookie: String
}

object Configs : Table<Config>("t_config") {
    val id = int("rowid").primaryKey().bindTo { it.id }
    val factorioUsername = varchar("factorio_username").bindTo { it.factorioUsername }
    val factorioToken = varchar("factorio_token").bindTo { it.factorioToken }
    val factorioSessionCookie = varchar("factorio_session_cookie").bindTo { it.factorioSessionCookie }
}

val configSchema = """
    CREATE TABLE IF NOT EXISTS t_config (
        factorio_username TEXT NOT NULL,
        factorio_token TEXT NOT NULL,
        factorio_session_cookie TEXT NOT NULL
    )
""".trimIndent()
