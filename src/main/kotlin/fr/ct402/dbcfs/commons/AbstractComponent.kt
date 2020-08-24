package fr.ct402.dbcfs.commons

import org.slf4j.Logger
import java.util.*

abstract class AbstractComponent {
    val logger: Logger
        get() = getLogger()
    val warnings = Stack<Exception>()

    fun warn(e: Exception) = warnings.push(e)

    fun getWarnings(): List<Exception> {
        val elems = warnings.toList()
        warnings.clear()
        return elems
    }

    fun transferWarnings(other: AbstractComponent) {
        warnings.addAll(other.warnings)
        other.warnings.clear()
    }
}