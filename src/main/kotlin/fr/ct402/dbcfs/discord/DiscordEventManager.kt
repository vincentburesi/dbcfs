package fr.ct402.dbcfs.discord

import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DiscordListener : ListenerAdapter() {
    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    override fun onReady(event: ReadyEvent) = logger.info("Discord interface successfully connected")
    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.author.isBot) {
            if (event.isFromType(ChannelType.PRIVATE)) {
                logger.info("[MESSAGE] ${event.message.contentDisplay ?: "<null>"}")
                event.channel.sendMessage("Coucou ${event.message.author.name ?: "machin"} ! Je ne suis pas encore correctement configuré, mais ça va venir ;)").queue()
            } else
                logger.info("[${event.guild.name}][${event.textChannel.name}] ${event.member?.effectiveName}: " + event.message.contentDisplay)
        }
    }
}

fun loadDiscordInterface(config: DiscordConfigProperties) {
    val token = config.token
    val listener = DiscordListener()

    val jda = JDABuilder.createDefault(token)
            .addEventListeners(listener)
            .build()
}
