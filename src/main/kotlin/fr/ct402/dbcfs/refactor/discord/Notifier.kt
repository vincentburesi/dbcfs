//package fr.ct402.dbcfs.refactor.discord
//
//import fr.ct402.dbcfs.refactor.commons.getLogger
//import kotlinx.coroutines.*
//import kotlinx.coroutines.sync.Mutex
//import kotlinx.coroutines.sync.withLock
//import net.dv8tion.jda.api.entities.Message
//import net.dv8tion.jda.api.events.message.MessageReceivedEvent
//import java.lang.Exception
//
//@Deprecated("Replace with new Notifier")
//class Notifier (
//        val event: MessageReceivedEvent
//) {
//    private val logger = getLogger()
//    private val mutex = Mutex()
//    private var nextMessage: String? = null
//    private var job: Job? = null
//    private var lastUpdate = System.nanoTime()
//    var message: Message? = null
//        private set
//
//    companion object {
//        const val intervalInSeconds = 3
//    }
//
//    infix fun print(e: Exception) {
//        error(e.message ?: "An unspecified error occured, please check the system logs")
//    }
//
//    fun success(str: String) =
//            updateInternal(":green_circle:   $str", true)
//
//    fun error(str: String) =
//            updateInternal(":red_circle:   $str", true) { logger.error(str) }
//
//    fun parseError() =
//            error("Could not parse your command : ${event.message.contentDisplay}")
//
//    fun print(
//            str: String,
//            force: Boolean = false,
//            log: () -> Unit = { logger.info(str) }
//    ) = updateInternal(str, force, log)
//
//    fun update(
//            str: String,
//            force: Boolean = false,
//            log: () -> Unit = { logger.info(str) }
//    ) = updateInternal(":yellow_circle:   $str", force, log)
//
//    private fun updateInternal(
//            str: String,
//            force: Boolean = false,
//            log: () -> Unit = { logger.info(str) }
//    ) {
//        log()
//        runBlocking {
//            mutex.withLock {
//                nextMessage = str
//            }
//
//            if (force) {
//                job?.cancel()
//                updater()
//            } else
//                job = job ?: GlobalScope.launch {
//                    val waitTime = calculateDelayInMiliseconds()
//                    if (waitTime > 0)
//                        delay(waitTime)
//                    updater()
//                }
//        }
//    }
//
//    private fun sendMessage(msg: String) {
//        if (message != null)
//            message!!.editMessage(msg).queue()
//        else
//            message = event.channel.sendMessage(msg).complete()
//    }
//
//    private val updater = suspend updater@{
//        mutex.withLock {
//            val next = nextMessage
//            if (next != null) {
//                nextMessage = null
//                sendMessage(next)
//            }
//            lastUpdate = System.nanoTime()
//            job = null
//        }
//    }
//
//    private suspend fun calculateDelayInMiliseconds(): Long {
//        val elapsedInNanoseconds = mutex.withLock {
//            System.nanoTime() - lastUpdate
//        }
//        return (intervalInSeconds * 1_000) - (elapsedInNanoseconds / 1_000_000)
//    }
//}