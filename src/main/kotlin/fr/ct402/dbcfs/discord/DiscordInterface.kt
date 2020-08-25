package fr.ct402.dbcfs.discord

import fr.ct402.dbcfs.CommandParser
import fr.ct402.dbcfs.commons.AbstractComponent
import fr.ct402.dbcfs.commons.discordAuthorizedFiles
import fr.ct402.dbcfs.manager.DiscordAuthManager
import fr.ct402.dbcfs.manager.ProfileManager
import fr.ct402.dbcfs.persist.DbLoader
import fr.ct402.dbcfs.persist.model.AuthorizedId
import fr.ct402.dbcfs.persist.model.AuthorizedIds
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.entity.add
import me.liuwj.ktorm.entity.removeIf
import me.liuwj.ktorm.entity.sequenceOf
import me.liuwj.ktorm.entity.toList
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.lang.Exception

@Component
@Configuration
class DiscordInterface(
        val config: DiscordConfigProperties,
        val commandParser: CommandParser,
        val profileManager: ProfileManager,
        val discordAuthManager: DiscordAuthManager
) : AbstractComponent() {
    val listener: ListenerAdapter = object : ListenerAdapter() {
        override fun onReady(event: ReadyEvent) = logger.info("Discord interface successfully connected")

        override fun onMessageReceived(event: MessageReceivedEvent) {
            if (discordAuthManager.isAuthorized(event)) {
                if (event.message.attachments.isEmpty())
                    handleCommand(event)
                else
                    handleAttachments(event)
            }
        }
    }

    companion object {
        private var jdaInternal: JDA? = null

        val jda: JDA
            get() = jdaInternal ?: throw IllegalStateException("JDA not initialized")
    }

    @EventListener(ApplicationReadyEvent::class)
    fun load() {
        logger.info("Loading discord interface")
        jdaInternal = JDABuilder.createDefault(config.token)
                .addEventListeners(listener)
                .build()
    }

    fun handleAttachments(event: MessageReceivedEvent) {
        event.message.attachments.forEach {
            val msg = try {
                if (it.fileName in discordAuthorizedFiles) {
                    if (profileManager.uploadConfigFile(it))
                        "Sucessfully added ${it.fileName}"
                    else
                        "Error adding ${it.fileName}: Current profile is not selected"
                } else
                    "Error cannot add ${it.fileName}, name does not match authorized (${discordAuthorizedFiles.reduce { acc, s -> "$acc, $s" }})"
            } catch (e: Exception) {
                "Error downloading ${it.fileName}: ${e.message}"
            }
            event.channel.sendMessage(msg).queue()
        }
    }

    fun handleCommand(event: MessageReceivedEvent) {
        logger.info("Message received raw : ${event.message.contentRaw}")
        logger.info("Message received displayed : ${event.message.contentDisplay}")
        logger.info("Message received stripped : ${event.message.contentStripped}")
        commandParser.parseCommand(event)
    }

}
