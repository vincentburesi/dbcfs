package fr.ct402.dbcfs.persist.model

import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.schema.*

interface AllowedId : Entity<AllowedId> {
    enum class IdType { USER, ROLE, CHANNEL }

    companion object : Entity.Factory<AllowedId>()

    var id: Int
    var name: String
    var discordId: String
    var idType: IdType
}

object AllowedIds : Table<AllowedId>("t_allowed_id") {
    val id = int("rowid").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
    val discordId = varchar("discord_id").bindTo { it.discordId }
    val idType = enum("id_type", typeRef<AllowedId.IdType>()).bindTo { it.idType }
}

val allowedIdSchema = """
    CREATE TABLE IF NOT EXISTS t_allowed_id (
        name TEXT NOT NULL,
        discord_id TEXT NOT NULL UNIQUE,
        id_type TEXT NOT NULL
    )
""".trimIndent()
