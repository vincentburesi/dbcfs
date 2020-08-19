package fr.ct402.dbcfs.discord

import fr.ct402.dbcfs.commons.discordAuthorizedFiles
import fr.ct402.dbcfs.commons.getLogger
import khttp.get
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger
import java.io.File
//
//class DiscordListener : ListenerAdapter() {
//    val logger: Logger = getLogger()
//
//    override fun onReady(event: ReadyEvent) = logger.info("Discord interface successfully connected")
//    override fun onMessageReceived(event: MessageReceivedEvent) {
//        if (!event.author.isBot && event.author.name == "sdg ct") {
//            if (event.isFromType(ChannelType.PRIVATE)) {
//                logger.info("[MESSAGE] ${event.message.contentDisplay ?: "<null>"}")
//                event.message.attachments.forEach {
//                    if (it.fileName in discordAuthorizedFiles) {
//                        logger.info("Trying to download to /mnt/test/${it?.fileName}")
//                        logger.info("URL: ${it?.proxyUrl}")
//
//                        //                    val file = File("/mnt/${it.fileName}")
//                        //                    val res = get(it.proxyUrl, stream = true)
//                        //                    for (chunk in res.contentIterator(1024))
//                        //                        file.appendBytes(chunk)
//
//                        //                    it?.retrieveInputStream()?.thenAccept { input ->
//                        //                        val file = File("/mnt/test/${it.fileName}")
//                        //                        file.outputStream().use { output ->
//                        //                            input.copyTo(output)
//                        //                        }
//                        //                    }
//
//                        it?.downloadToFile(File("/mnt/test/${it.fileName}"))?.thenAccept { file ->
//                            logger.info("Saved attachment to " + file.getName())
//                        }?.exceptionally { t ->
//                            logger.error("Error could not download attachement : ${t.localizedMessage}")
//                            null
//                        }
//                        logger.info("Success : /mnt/test/${it?.fileName}")
//                    }
//                }
//                event.channel.sendMessage("Coucou ${event.message.author.name ?: "machin"} ! Je ne suis pas encore correctement configuré, mais ça va venir ;)").queue()
//            } else
//                logger.info("[${event.guild.name}][${event.textChannel.name}] ${event.member?.effectiveName}: " + event.message.contentDisplay)
//        }
//    }
//}
//
