package fr.ct402.dbcfs.commons

import kotlin.math.min

/**
 * Returns a positive value if s1 is greater than s2
 */
fun compareVersionStrings(s1: String, s2: String): Int {
    val n1 = s1.split('.').map { it.toInt() }
    val n2 = s2.split('.').map { it.toInt() }

    for (i in 0 until min(n1.size, n2.size))
        if (n1[i] - n2[i] != 0)
            return n1[i] - n2[i]

    return 0;
}

fun String.isGreaterVersionString(str: String) = compareVersionStrings(this, str) > 0
fun String.isGreaterOrEqualVersionString(str: String) = compareVersionStrings(this, str) >= 0
fun String.isLesserVersionString(str: String) = compareVersionStrings(this, str) < 0
fun String.isLesserOrEqualVersionString(str: String) = compareVersionStrings(this, str) <= 0
