@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package ai.solace.core.actor.examples

import ai.solace.core.actor.Actor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.uuid.Uuid

/**
 * An actor that filters messages based on a predicate.
 *
 * This actor provides ports for:
 * - Receiving input messages
 * - Outputting messages that pass the filter
 * - Outputting messages that fail the filter (optional)
 *
 * @param T The type of messages this filter processes.
 * @param id Unique identifier of the actor, defaults to a random UUID.
 * @param name Name of the actor, defaults to "Filter".
 * @param scope Coroutine scope used by the actor, defaults to a new scope with default dispatcher and supervisor job.
 * @param predicate The function used to determine if a message passes the filter.
 * @param includeRejectedPort Whether to create a port for rejected messages.
 */
class Filter<T : Any>(
    id: String = Uuid.random().toString(),
    name: String = "Filter",
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    private val predicate: (T) -> Boolean,
    private val includeRejectedPort: Boolean = false,
    private val messageClass: kotlin.reflect.KClass<T>
) : Actor(id, name, scope) {

    companion object {
        /**
         * Port name for receiving input messages.
         */
        const val INPUT_PORT = "input"

        /**
         * Port name for sending messages that pass the filter.
         */
        const val ACCEPTED_PORT = "accepted"

        /**
         * Port name for sending messages that fail the filter.
         */
        const val REJECTED_PORT = "rejected"
    }

    /**
     * Initializes the actor by creating the input and output ports.
     */
    suspend fun initialize() {
        // Create input port for receiving messages
        createPort(
            name = INPUT_PORT,
            messageClass = messageClass,
            handler = { message -> filterMessage(message) },
            bufferSize = 10  // Use a positive buffer size
        )

        // Create output port for accepted messages
        createPort(
            name = ACCEPTED_PORT,
            messageClass = messageClass,
            handler = { /* This port is only used for output, so no handler is needed */ },
            bufferSize = 10  // Use a positive buffer size
        )

        // Create output port for rejected messages if requested
        if (includeRejectedPort) {
            createPort(
                name = REJECTED_PORT,
                messageClass = messageClass,
                handler = { /* This port is only used for output, so no handler is needed */ },
                bufferSize = 10  // Use a positive buffer size
            )
        }
    }

    /**
     * Filters the input message based on the predicate.
     *
     * @param message The input message to filter.
     */
    private suspend fun filterMessage(message: T) {
        if (predicate(message)) {
            // Message passes the filter, send to accepted port
            val acceptedPort = getPort(ACCEPTED_PORT, messageClass)
            acceptedPort?.send(message)
        } else if (includeRejectedPort) {
            // Message fails the filter, send to rejected port if it exists
            val rejectedPort = getPort(REJECTED_PORT, messageClass)
            rejectedPort?.send(message)
        }
    }

    /**
     * Starts the actor, initializing it if necessary.
     */
    override suspend fun start() {
        // Initialize the actor if it hasn't been initialized yet
        if (getPort(INPUT_PORT, messageClass) == null) {
            initialize()
        }

        // Call the parent start method
        super.start()
    }
}
