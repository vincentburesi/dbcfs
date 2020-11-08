package fr.ct402.dbcfs.refactor.commons

import fr.ct402.dbcfs.Notifier
import fr.ct402.dbcfs.error
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

enum class SizeUnit (val unit: String, val multiplier: Long) {
    BYTE("B", 1),
    KILO("kB", 1024),
    MEGA("MB", 1024 * 1024),
    GIGA("GB", 1024 * 1024 * 1024),
}
val sizeUnits = SizeUnit.values().toList().sortedByDescending { it.multiplier }
fun fileSizeAsString(size: Long) =
        sizeUnits.first { it.multiplier < size }.let { "${size / it.multiplier}${it.unit}" }

val possibleConfigFiles = setOf("server-settings.json", "map-gen-settings.json", "map-settings.json") //TODO Check the names
const val baseDataDir = "/mnt"
const val factorioExecutableRelativeLocation = "factorio/bin/x64/factorio"
const val profileRelativeModDirectory = "mods"

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
const val linkValidityMention = "*Links will be valid for the next $tokenValidityInMinutes minutes*"
const val tokenLength = 32
val tokenAllowedChars = ('A'..'Z').joinToString("") + ('a'..'z').joinToString("") + ('0'..'9').joinToString("")

fun <R> Notifier.launchAsCoroutine(block: suspend () -> R) {
    GlobalScope.launch {
        try {
            block()
        } catch (e: Exception) {
            this@launchAsCoroutine error e
        }
    }
}

class NoCurrentProfileException: RuntimeException("No profile is currently selected, please select or create a profile first (See create profile or swap)")
class ProfileNotFoundException(name: String): RuntimeException("No profile found matching this name: $name")
class ProfileNameNotAvailableException(name: String): RuntimeException("A profile already exists with this name: $name")
class MissingArgumentException(cmd: String, argName: String): RuntimeException("$cmd: Missing $argName argument")
class InvalidArgumentException(argName: String, possibleValues: String): RuntimeException("$argName is invalid, possible values are $possibleValues")
class MatchingVersionNotFound(version: String): RuntimeException("Could not find matching version for $version. Try to sync the server or check factorio version list")
class FactorioApiErrorException(msg: String): RuntimeException("An error occured during the Factorio API calls - $msg")
class ModNotFoundException(name: String): RuntimeException("No mod found matching this name: $name. Try to sync the server or check factorio mod portal")
class GameReleaseNotFoundException(): RuntimeException("No matching game release was found")
class ModReleaseNotFoundException(modName: String, version: String? = null): RuntimeException("No mod release found matching $modName${ if (version != null) " version $version" else "" }")
