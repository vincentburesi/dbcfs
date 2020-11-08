package fr.ct402.dbcfs

import fr.ct402.dbcfs.Notifier.Status.*
import fr.ct402.dbcfs.persist.model.ModRelease
import fr.ct402.dbcfs.persist.model.Profile
import fr.ct402.dbcfs.refactor.commons.tokenValidityInMinutes

const val listPoint = ":small_blue_diamond:"
const val discordMessageLimit = 2000

/**
 * Prints running message
 */
fun Notifier.running(msg: String) = message(msg, RUNNING)

/**
 * Prints success message
 */
fun Notifier.success(msg: String) = message(msg, SUCCESS)

/**
 * Prints error message and flush. Blocking
 */
infix fun Notifier.error(msg: String) = message(msg, ERROR).flush()

/**
 * Prints error message and flush. Blocking
 */
infix fun Notifier.error(e: Exception) =
        error(e.message ?: "An unknown ${e.javaClass.simpleName} occurred, please check the logs for details")

/**
 * Prints empty list message
 */
fun Notifier.printEmpty() = message("*Empty*", NO_PREFIX)

/**
 * Prints list of elements truncated for discord message size. Blocking
 */
fun Notifier.printTruncatedList(list: List<String>) {
    if (list.isEmpty()) return printEmpty().queue()
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
    if (list.isEmpty()) return printEmpty().queue()
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

fun Notifier.success(fileName: String, profile: Profile, domain: String) =
        success("**$fileName** $domain/file/${profile.name}/${profile.token}/$fileName\n"
                + "*Link will be valid for the next $tokenValidityInMinutes minutes*")


fun Notifier.parseError() =
        error("Could not parse your command : ${event.message.contentDisplay}")


fun Notifier.printModReleases(list: List<ModRelease>, all: Boolean = false) {
    val strings = list.map {
        "$listPoint **${it.mod.name}** - ${it.version} - *${it.mod.summary}*"
    }.reversed()
    if (all) printFullList(strings) else printTruncatedList(strings)
}
