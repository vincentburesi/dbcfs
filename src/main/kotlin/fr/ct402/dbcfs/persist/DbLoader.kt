package fr.ct402.dbcfs.persist

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.asIterable
import me.liuwj.ktorm.support.sqlite.SQLiteDialect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.sql.SQLException

@Component
@Configuration
class DbLoader (private val config: DbConfigProperties) {
    val logger: Logger = LoggerFactory.getLogger(this.javaClass)
    val database = Database.connect(
            url = "jdbc:sqlite:${config.path}",
            driver = "org.sqlite.JDBC",
            dialect = SQLiteDialect()
    )

    @EventListener(ApplicationReadyEvent::class)
    fun load() {
        logger.info("Initializing DB")
        setup()
    }

    fun setup() {
        database.useConnection { connection ->
            val sql = """
                CREATE TABLE IF NOT EXISTS t_profile (
                    id INTEGER NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL
                )
            """.trimIndent()

            connection.prepareStatement(sql).use {
                try {
                    it.execute()
                    logger.info(sql)
                } catch (e: SQLException) {
                    logger.error(e.localizedMessage)
                }
            }
        }
    }
}