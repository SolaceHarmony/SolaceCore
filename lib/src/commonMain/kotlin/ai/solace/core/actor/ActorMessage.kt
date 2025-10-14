@file:OptIn(ExperimentalUuidApi::class)
package ai.solace.core.actor

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import ai.solace.core.kernel.channels.ports.Port
import kotlinx.datetime.Clock
/**
 * Message priority levels for the actor system.
 * Priority affects how messages are processed in the system.
 */
enum class MessagePriority {
    /** High priority messages are processed first */
    HIGH,

    /** Default priority level for messages */
    NORMAL,

    /** Low priority messages are processed when resources are available */
    LOW
}

/**
 * Represents a message in the actor system.
 * Messages are immutable data carriers between actors.
 *
 * @param T The type of payload this message carries
 * @property correlationId Unique identifier for tracking message chains
 * @property payload The actual content being transmitted
 * @property sender Identifier of the sending actor (optional)
 * @property timestamp When the message was created
 * @property priority Message processing priority
 * @property metadata Additional contextual information
 */
data class ActorMessage<out T : Any>(
    val correlationId: String = Uuid.random().toString(),
    val payload: T,
    val sender: String? = null,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val priority: MessagePriority = MessagePriority.NORMAL,
    val metadata: Map<String, Any> = emptyMap()
) {
    companion object {
        /**
         * Creates a message with high priority.
         * @param T The type of payload
         */
        fun <T : Any> highPriority(payload: T, sender: String? = null): ActorMessage<T> =
            ActorMessage(
                payload = payload,
                sender = sender,
                priority = MessagePriority.HIGH
            )

        /**
         * Creates a message with specific metadata.
         * @param T The type of payload
         */
        fun <T : Any> withMetadata(payload: T, metadata: Map<String, Any>): ActorMessage<T> =
            ActorMessage(
                payload = payload,
                metadata = metadata
            )

        /**
         * Creates a message between specific actors.
         * @param T The type of payload
         */
        fun <T : Any> between(payload: T, sender: String, metadata: Map<String, Any> = emptyMap()): ActorMessage<T> =
            ActorMessage(
                payload = payload,
                sender = sender,
                metadata = metadata
            )
    }
}

/**
 * Base class for handling actor messages with type safety.
 * @param T The type of payload that the ActorMessage carries
 */
abstract class ActorMessageHandler<T : Any> : Port.MessageHandler<ActorMessage<T>, ActorMessage<T>> {
    /**
     * Processes the given actor message and returns a new or modified actor message.
     * @param message The ActorMessage to process
     * @return A processed ActorMessage
     */
    abstract suspend fun processMessage(message: ActorMessage<T>): ActorMessage<T>

    /**
     * Handles the provided actor message asynchronously and returns the processed message.
     * @param message The message to be processed, of type ActorMessage<T>
     * @return The processed message, also of type ActorMessage<T>
     */
    final override suspend fun handle(message: ActorMessage<T>): ActorMessage<T> = processMessage(message)
}
