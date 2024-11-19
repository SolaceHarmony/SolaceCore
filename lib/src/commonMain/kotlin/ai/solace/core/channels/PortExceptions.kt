package ai.solace.core.channels

/**
 * Custom exceptions for the port system
 */
sealed class PortException(message: String) : Exception(message)

class PortConnectionException(
    val sourceId: String,
    val targetId: String,
    message: String
) : PortException("Failed to connect port $sourceId to $targetId: $message")

class PortNotFoundException(
    val portId: String
) : PortException("Port not found: $portId")

class PortTypeException(
    val expectedType: String,
    val actualType: String
) : PortException("Port type mismatch: expected $expectedType but was $actualType")