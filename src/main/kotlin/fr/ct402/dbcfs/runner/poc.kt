package fr.ct402.dbcfs.runner

import fr.ct402.dbcfs.commons.AbstractComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ExecLoader: AbstractComponent() {

    @EventListener(ApplicationReadyEvent::class)
    fun load() {
        logger.info("Loading executable runner")
        val p = Runtime.getRuntime().exec(arrayOf("ls"))
        var i = 0
        while (p.isAlive)
            if (++i < 5)
                logger.info("ALIVE")
        logger.info("DEAD")
    }
}