package ai.solace.core.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Provides logging functionality throughout the application.
 */
object LoggerProvider {
    /**
     * Gets a logger for the specified class.
     *
     * @param clazz The class to get the logger for.
     * @return The logger instance.
     */
    fun getLogger(clazz: Class<*>): Logger = LoggerFactory.getLogger(clazz)
    /**
     * Gets a logger using the specified name.
     *
     * @param name The name for the logger.
     * @return The logger instance.
     */
    fun getLogger(name: String): Logger = LoggerFactory.getLogger(name)
}

/**
 * Extension property to easily get a logger for a class.
 */
val Any.logger: Logger
    get() = LoggerProvider.getLogger(this.javaClass)
