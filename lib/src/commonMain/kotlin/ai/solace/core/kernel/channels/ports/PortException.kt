package ai.solace.core.kernel.channels.ports

/**
 * Base class for all port-related exceptions.
 * Provides a common hierarchy for handling port-specific errors.
 */
internal open class PortException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * Validation exception for port operations
     */
    class Validation(
        message: String,
        cause: Throwable? = null
    ) : PortException(message, cause)
}

/**
 * Thrown when a port connection cannot be established or fails.
 * Contains detailed information about the source and target ports involved.
 *
 * @property sourceId Identifier of the source port
 * @property targetId Identifier of the target port
 * @property details Additional error details
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
    companion object {
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
 * Exception thrown when sending a message fails.
 */
internal class SendMessageException(
    message: String,
    cause: Throwable? = null
) : PortException(message, cause)