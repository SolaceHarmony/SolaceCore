package ai.solace.core.actor.message

import java.util.*

enum class MessagePriority {
    HIGH, NORMAL, LOW
}

/**
 * Improved message structure with additional metadata and type safety
 */
data class ActorMessage<T>(
    val correlationId: String = UUID.randomUUID().toString(),
    val type: String,
    val payload: T,
    val sender: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val priority: MessagePriority = MessagePriority.NORMAL,
    val metadata: Map<String, Any> = emptyMap()
) {
    companion object {
        fun <T> createHighPriority(type: String, payload: T): ActorMessage<T> =
            ActorMessage(type = type, payload = payload, priority = MessagePriority.HIGH)

        fun <T> createWithMetadata(type: String, payload: T, metadata: Map<String, Any>): ActorMessage<T> =
            ActorMessage(type = type, payload = payload, metadata = metadata)
    }
}