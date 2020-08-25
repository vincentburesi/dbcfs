package fr.ct402.dbcfs.commons

import fr.ct402.dbcfs.discord.Notifier
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.min
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

fun compareVersionStrings(s1: String, s2: String): Int {
    val n1 = s1.split('.').map { it.toInt() }
    val n2 = s2.split('.').map { it.toInt() }

    for (i in 0 until min(n1.size, n2.size))
        if (n1[i] - n2[i] != 0)
            return n1[i] - n2[i]

    return 0;
}

val discordAuthorizedFiles = setOf("server-settings.json", "map-gen-settings.json", "map-settings.json") //TODO Check the names
const val baseDataDir = "/mnt"
const val factorioExecutableRelativeLocation = "factorio/bin/x64/factorio"

const val orderDbLoad = 1
const val orderAfterDbLoad = 2

fun Any.getLogger() = LoggerFactory.getLogger(this.javaClass.simpleName.takeWhile { it != '$' })!!
fun <T> Iterator<T>.nextOrNull() = if (hasNext()) next() else null

const val debugMode = true
inline infix fun <T> (() -> T).catch(c: () -> T) =
        try {
            this()
        } catch(e: Exception) {
            e.getLogger().error(e.message)
            if (debugMode) e.printStackTrace()
            c()
        }

fun <R> Throwable.multicatch(vararg classes: KClass<*>, block: () -> R): R {
    if (classes.any { this::class.isSubclassOf(it) }) {
        return block()
    } else throw this
}

fun parseDateTime(str: String): LocalDateTime =
        LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME)
fun printDateTime(dateTime: LocalDateTime): String =
        dateTime.format(DateTimeFormatter.ISO_DATE_TIME)

const val tokenValidityInMinutes = 60L
const val tokenLength = 64
val tokenAllowedChars = ('A'..'Z').joinToString("") + ('a'..'z').joinToString("") + ('0'..'9').joinToString("")

fun <R> Notifier.launchAsCoroutine(block: suspend () -> R) {
    GlobalScope.launch {
        try {
            block()
        } catch (e: Exception) {
            this@launchAsCoroutine print e
        }
    }
}

class NoCurrentProfileException: RuntimeException("No profile is currently selected, please select or create a profile first (See create profile or swap)")
class ProfileNotFoundException(name: String): RuntimeException("No profile found matching this name: $name")
class MissingArgumentException(cmd: String, argName: String): RuntimeException("$cmd: Missing $argName argument")
class MatchingVersionNotFound(version: String): RuntimeException("Could not find matching version for $version. Try to sync the server or check factorio version list")

