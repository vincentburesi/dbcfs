package fr.ct402.dbcfs

import fr.ct402.dbcfs.Notifier.Status.*
import fr.ct402.dbcfs.refactor.discord.discordMessageLimit

/**
 * Adds given message to Notifier queue
 * Does not block, does not update status
 */
fun Notifier.print(msg: String) = message(msg).queue()

/**
 * Set notifier status to success and flush. Blocking
 */
fun Notifier.success() = status(SUCCESS).flush()

/**
 * Prints error message. Blocking
 */
fun Notifier.error(msg: String) = message(msg, ERROR).flush()

/**
 * Prints error message. Blocking
 */
fun Notifier.error(e: Exception) =
        error(e.message ?: "An unknown ${e.javaClass.simpleName} occurred, please check the logs for details")

/**
 * Prints empty list message. Blocking
 */
fun Notifier.printEmpty() = message("*Empty*", NO_PREFIX).flush()

/**
 * Prints list of elements truncated for discord message size. Blocking
 */
fun Notifier.printTruncatedList(list: List<String>) {
    if (list.isEmpty()) return printEmpty()
    var cumulatedSize = 0

    list.takeWhile {
        cumulatedSize += it.length + 1
        cumulatedSize < discordMessageLimit
    }.joinToString(separator = "\n").let { message(it).flush() }
}

/**
 * Prints list of elements as multiple discord messages.
 * Prints directly through event channel as new messages, ignoring usual [Notifier] system.
 * Blocking
 */
fun Notifier.printFullList(list: List<String>) {
    if (list.isEmpty()) return printEmpty()
    val toSend = arrayListOf<String>()
    var buffer = ""

    list.forEach {
        if (buffer.length + it.length + 1 <= discordMessageLimit)
            buffer += "\n$it"
        else { // We can't group this message with the previous ones
            // Flush current buffer
            if (buffer.isNotBlank())
                toSend.add(buffer)
            buffer = ""

            if (it.length <= discordMessageLimit)
                buffer = it // Add next message...
            else
                toSend.addAll(it.chunked(discordMessageLimit)) // ...or chunk it if too big
        }
    }

    if (buffer.isNotBlank())
        toSend.add(buffer)

    toSend.forEach {
        event.channel.sendMessage(it).complete()
    }
}
