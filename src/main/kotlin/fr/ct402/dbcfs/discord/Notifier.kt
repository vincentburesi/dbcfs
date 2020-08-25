package fr.ct402.dbcfs.discord

import fr.ct402.dbcfs.commons.getLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.lang.Exception

class Notifier (
        val event: MessageReceivedEvent
) {
    private val logger = getLogger()
    private val mutex = Mutex()
    private var nextMessage: String? = null
    private var job: Job? = null
    private var lastUpdate = System.nanoTime()
//    var event: MessageReceivedEvent? = null
//        private set
    var message: Message? = null
        private set

    companion object {
        const val intervalInSeconds = 3
    }

//    constructor(message: Message) {
//        this.message = message
//    }

//    constructor(event: MessageReceivedEvent) {
//        this.event = event
//    }

    infix fun print(e: Exception) {
        error(e.message ?: "An unspecified error occured, please check the system logs")
    }

    fun success(str: String) =
            update(str, true)

    fun error(str: String) =
            update(str, true) { logger.error(str) }

    fun missingArgument() =
            error("Missing argument for command : ${event.message.contentDisplay}")

    fun parseError() =
            error("Could not parse your command : ${event.message.contentDisplay}")

    fun update(
            str: String,
            force: Boolean = false,
            log: () -> Unit = { logger.info(str) }
    ) {
        log()
        runBlocking {
            mutex.withLock {
                nextMessage = str
            }

            if (force) {
                job?.cancel()
                updater()
            } else
                job = job ?: GlobalScope.launch {
                    val waitTime = calculateDelayInMiliseconds()
                    if (waitTime > 0)
                        delay(waitTime)
                    updater()
                }
        }
    }

    private fun sendMessage(msg: String) {
        if (message != null)
            message!!.editMessage(msg).queue()
        else
            message = event!!.channel.sendMessage(msg).complete()
    }

    private val updater = suspend updater@{
        mutex.withLock {
            val next = nextMessage
            if (next != null) {
                nextMessage = null
                sendMessage(next)
            }
            lastUpdate = System.nanoTime()
            job = null
        }
    }

    private suspend fun calculateDelayInMiliseconds(): Long {
        val elapsedInNanoseconds = mutex.withLock {
            System.nanoTime() - lastUpdate
        }
        return (intervalInSeconds * 1_000) - (elapsedInNanoseconds / 1_000_000)
    }
}