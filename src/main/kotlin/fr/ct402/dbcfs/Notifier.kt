package fr.ct402.dbcfs

import fr.ct402.dbcfs.refactor.commons.getLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class Notifier (
        val event: MessageReceivedEvent,
) {
    companion object {
        const val intervalInSeconds = 3
    }

    enum class Status(val prefix: String) {
        NO_PREFIX(""),
        SUCCESS(":green_circle:  "),
        ERROR(":red_circle:  "),
        RUNNING(":yellow_circle:  "),
        AWAITING_REPLY(":blue_circle:  "),
    }

    /**
     * Short term lock preventing concurrent message read/write
     * Should never stay locked for a long period
     */
    private val messageMutex = Mutex()

    /**
     * Prevents sender job concurrent edit/execution
     * Can stay locked for a while and block execution
     */
    private val senderMutex = Mutex()

    private val logger = getLogger()
    private var status = Status.NO_PREFIX
    private var message: String? = null
    private var discordMessage: Message? = null
    private var senderJob: Job? = null
    private var lastUpdate = System.nanoTime()

    /**
     * Changes the status
     * Should be followed by [queue] or [flush]
     */
    fun status(status: Status): Notifier {
        runBlocking {
            messageMutex.withLock {
                this@Notifier.status = status
            }
        }
        return this
    }

    /**
     * Changes the message content and status
     * Should be followed by [queue] or [flush]
     */
    fun message(str: String, status: Status? = null): Notifier {
        logger.trace(str)
        runBlocking {
            messageMutex.withLock {
                message = str
                if (status != null)
                    this@Notifier.status = status
            }
        }
        return this
    }

    /**
     * Cancels the scheduled sender job if any and update discord immediately and synchronously,
     * blocking the calling thread
     */
    fun flush() {
        runBlocking {
            senderMutex.withLock {
                senderJob?.cancel()
                message()
            }
        }
    }

    /**
     * Update discord asynchronously, respecting a throttle to avoid overloading the API
     * It is possible (and intended) that some messages are delayed and/or skipped
     */
    fun queue() {
        GlobalScope.launch {
            senderMutex.withLock {
                val delayLength = calculateDelayInMiliseconds()

                senderJob = senderJob ?: GlobalScope.launch {
                    if (delayLength > 0)
                        delay(delayLength)
                    senderMutex.withLock { message() }
                }
            }
        }
    }

    private suspend fun getFormattedMessage() =
            messageMutex.withLock {
                "${status.prefix}$message"
            }

    /**
     * Should be called inside a senderMutex lock
     */
    private suspend fun message() {
        val toSent = getFormattedMessage()

        logger.trace("Sending discord message : $toSent")

        if (discordMessage != null)
            discordMessage!!.editMessage(toSent).complete()
        else
            discordMessage = event.channel.sendMessage(toSent).complete()

        lastUpdate = System.nanoTime()
        senderJob = null
    }

    /**
     * Should be called inside a senderMutex lock
     */
    private fun calculateDelayInMiliseconds(): Long {
        val elapsedInNanoseconds = System.nanoTime() - lastUpdate
        return (intervalInSeconds * 1_000) - (elapsedInNanoseconds / 1_000_000)
    }
}