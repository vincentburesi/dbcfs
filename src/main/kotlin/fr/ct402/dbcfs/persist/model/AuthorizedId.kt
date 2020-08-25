package fr.ct402.dbcfs.persist.model

import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.*

interface AuthorizedId : Entity<AuthorizedId> {
    enum class IdType { USER, ROLE }

    companion object : Entity.Factory<AuthorizedId>()

    val id: Int
    var name: String
    var discordId: String
    var idType: IdType
}

object AuthorizedIds : Table<AuthorizedId>("t_authorized_id") {
    val id = int("rowid").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
    val discordId = varchar("discord_id").bindTo { it.discordId }
    val idType = enum("id_type", typeRef<AuthorizedId.IdType>()).bindTo { it.idType }
}

val authorizedIdSchema = """
    CREATE TABLE IF NOT EXISTS t_authorized_id (
        name TEXT NOT NULL,
        discord_id TEXT NOT NULL UNIQUE,
        id_type TEXT NOT NULL
    )
""".trimIndent()
