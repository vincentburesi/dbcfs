package fr.ct402.dbcfs.refactor.persist

import fr.ct402.dbcfs.refactor.commons.AbstractComponent
import fr.ct402.dbcfs.refactor.commons.Config
import fr.ct402.dbcfs.refactor.commons.orderDbLoad
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.support.sqlite.SQLiteDialect
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Configuration
class DbLoader (private val config: Config): AbstractComponent() {
    val database = Database.connect(
            url = "jdbc:sqlite:${config.db.path}",
            driver = "org.sqlite.JDBC",
            dialect = SQLiteDialect()
    )

    @Order(orderDbLoad)
    @EventListener(ApplicationReadyEvent::class)
    fun load() {
        logger.info("Initializing DB")
        setup(database)
        logger.info("DB initialized")
    }

}