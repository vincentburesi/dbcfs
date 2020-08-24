package fr.ct402.dbcfs.persist

import fr.ct402.dbcfs.commons.AbstractComponent
import fr.ct402.dbcfs.commons.getLogger
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.asIterable
import me.liuwj.ktorm.support.sqlite.SQLiteDialect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.sql.SQLException

@Component
@Configuration
class DbLoader (private val config: DbConfigProperties): AbstractComponent() {
    val database = Database.connect(
            url = "jdbc:sqlite:${config.path}",
            driver = "org.sqlite.JDBC",
            dialect = SQLiteDialect()
    )

    @Order(1)
    @EventListener(ApplicationReadyEvent::class)
    fun load() {
        logger.info("Initializing DB")
        setup(database)
        logger.info("DB initialized")
    }

}