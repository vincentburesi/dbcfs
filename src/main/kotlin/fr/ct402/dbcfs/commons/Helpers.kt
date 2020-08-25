package fr.ct402.dbcfs.commons

import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.min

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

fun parseDateTime(str: String): LocalDateTime =
        LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME)
fun printDateTime(dateTime: LocalDateTime): String =
        dateTime.format(DateTimeFormatter.ISO_DATE_TIME)

const val tokenValidityInMinutes = 60L
const val tokenLength = 64
val tokenAllowedChars = ('A'..'Z').joinToString("") + ('a'..'z').joinToString("") + ('0'..'9').joinToString("")


