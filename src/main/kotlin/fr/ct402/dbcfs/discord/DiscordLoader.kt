package fr.ct402.dbcfs.discord

import fr.ct402.dbcfs.commons.getLogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

//@Component
//@Configuration
//class DiscordLoader (private val config: DiscordConfigProperties) {
//    val logger = getLogger()
//
//    @EventListener(ApplicationReadyEvent::class)
//    fun load() {
//        logger.info("Loading discord interface")
//        loadDiscordInterface(config)
//    }
//}