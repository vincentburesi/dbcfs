package fr.ct402.dbcfs.discord

import fr.ct402.dbcfs.commons.getLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.dv8tion.jda.api.entities.Message

class Notifier (
        val discordMessage: Message
) {
    val logger = getLogger()
    private val mutex = Mutex()

    private var currentMessage: String = discordMessage.contentRaw
    private var nextMessage: String? = null
    private var job: Job? = null
    private var lastUpdate = System.nanoTime()

    private val updater = suspend updater@{
        mutex.withLock {
            val next = nextMessage
            if (next != null && next != currentMessage) {
                logger.warn("Updating: $next")
                currentMessage = next
                nextMessage = null
                discordMessage.editMessage(next).queue()
            }
            lastUpdate = System.nanoTime()
            job = null
        }
    }

    private suspend fun calculateDelayInMiliseconds(): Long {
        val elapsedInNanoseconds = mutex.withLock {
            System.nanoTime() - lastUpdate
        }
        logger.warn("Elapsed: $elapsedInNanoseconds ns, delay: ${(intervalInSeconds * 1000) - (elapsedInNanoseconds / 1000)}")
        return (intervalInSeconds * 1_000) - (elapsedInNanoseconds / 1_000_000)
    }

    fun update(str: String, force: Boolean = false) {
        logger.info(str)
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

    companion object {
        const val intervalInSeconds = 3
    }
}