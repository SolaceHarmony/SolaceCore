package ai.solace.core.kernel.channels.ports

/**
 * Base exception class for errors related to port operations.
 *
 * @param message Detailed message explaining the cause of the exception.
 * @param cause The original cause of this exception, if any.
 */
internal open class PortException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * An exception specific to validation failures within port operations.
     *
     * This exception is thrown during the validation process of port operations,
     * such as type conversion, message handling, or protocol adaptation. It extends
     * from `PortException` to signify errors specific to the port's validation phase.
     *
     * @param message Detailed message describing the validation failure.
     * @param cause The cause of the validation failure, if any.
     */
    class Validation(
        message: String,
        cause: Throwable? = null
    ) : PortException(message, cause)
}

/**
 * Exception thrown when a port connection fails.
 *
 * @property sourceId The identifier of the source port.
 * @property targetId The identifier of the target port.
 * @property details Additional details about the connection failure.
 */
internal class PortConnectionException(
    val sourceId: String,
    val targetId: String,
    message: String,
    details: Map<String, Any> = emptyMap(),
    cause: Throwable? = null
) : PortException(
    buildErrorMessage(sourceId, targetId, message, details),
    cause
) {
    /**
     * Companion object for PortConnectionException that provides utility functions.
     */
    companion object {
        /**
         * Builds an error message for a port connection exception.
         *
         * @param sourceId The ID of the source port.
         * @param targetId The ID of the target port.
         * @param message A custom message describing the error.
         * @param details A map containing additional details about the error.
         * @return A formatted error message string.
         */
        private fun buildErrorMessage(
            sourceId: String,
            targetId: String,
            message: String,
            details: Map<String, Any>
        ): String = buildString {
            append("Failed to connect port $sourceId to $targetId: $message")
            if (details.isNotEmpty()) {
                append("\nDetails:")
                details.forEach { (key, value) ->
                    append("\n  $key: $value")
                }
            }
        }
    }
}

/**
 * Exception thrown when there is an error sending a message through a port.
 *
 * @param message The detail message for this exception.
 * @param cause The cause of this exception (optional).
 */
internal class SendMessageException(
    message: String,
    cause: Throwable? = null
) : PortException(message, cause)