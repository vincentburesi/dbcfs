package fr.ct402.dbcfs.commons

import org.slf4j.LoggerFactory
import kotlin.math.min

fun compareVersionStrings(s1: String, s2: String): Int {
    val n1 = s1.split('.').map { it.toInt() }
    val n2 = s2.split('.').map { it.toInt() }

    for (i in 0 until min(n1.size, n2.size))
        if (n1[i] - n2[i] != 0)
            return n1[i] - n2[i]

    return 0;
}

val baseDataDir = "/mnt"

fun Any.getLogger() = LoggerFactory.getLogger(this.javaClass.simpleName.takeWhile { it != '$' })!!
