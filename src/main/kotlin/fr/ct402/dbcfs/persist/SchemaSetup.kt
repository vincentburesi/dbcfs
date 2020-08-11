package fr.ct402.dbcfs.persist

import fr.ct402.dbcfs.persist.model.gameVersionSchema
import me.liuwj.ktorm.database.Database
import java.sql.SQLException

fun setup(database: Database) {
    arrayOf(gameVersionSchema).forEach { database.exec(it) }
}

fun Database.exec(statement: String) {
    useConnection { connection ->
        connection.prepareStatement(statement).use {
            try {
                it.execute()
            } catch (e: SQLException) {
                logger.error(e.localizedMessage)
            }
        }
    }
}
