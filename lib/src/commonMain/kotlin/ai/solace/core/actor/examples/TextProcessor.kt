@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package ai.solace.core.actor.examples

import ai.solace.core.actor.Actor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.uuid.Uuid

/**
 * An actor that processes text input by applying various transformations.
 *
 * This actor provides ports for:
 * - Receiving text input
 * - Outputting processed text
 *
 * @param id Unique identifier of the actor, defaults to a random UUID.
 * @param name Name of the actor, defaults to "TextProcessor".
 * @param scope Coroutine scope used by the actor, defaults to a new scope with default dispatcher and supervisor job.
 */
class TextProcessor(
    id: String = Uuid.random().toString(),
    name: String = "TextProcessor",
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    private val transformations: List<(String) -> String> = listOf()
) : Actor(id, name, scope) {

    companion object {
        /**
         * Port name for receiving text input.
         */
        const val INPUT_PORT = "input"

        /**
         * Port name for sending processed text output.
         */
        const val OUTPUT_PORT = "output"

        /**
         * Transformation that converts text to uppercase.
         */
        val TO_UPPERCASE: (String) -> String = { it.uppercase() }

        /**
         * Transformation that converts text to lowercase.
         */
        val TO_LOWERCASE: (String) -> String = { it.lowercase() }

        /**
         * Transformation that trims whitespace from the beginning and end of text.
         */
        val TRIM: (String) -> String = { it.trim() }

        /**
         * Transformation that removes all whitespace from text.
         */
        val REMOVE_WHITESPACE: (String) -> String = { it.replace("\\s".toRegex(), "") }

        /**
         * Transformation that reverses text.
         */
        val REVERSE: (String) -> String = { it.reversed() }
    }

    /**
     * Initializes the actor by creating the input and output ports.
     */
    suspend fun initialize() {
        // Create input port for receiving text
        createPort(
            name = INPUT_PORT,
            messageClass = String::class,
            handler = { text -> processText(text) },
            bufferSize = 10  // Use a positive buffer size
        )

        // Create output port for sending processed text
        createPort(
            name = OUTPUT_PORT,
            messageClass = String::class,
            handler = { /* This port is only used for output, so no handler is needed */ },
            bufferSize = 10  // Use a positive buffer size
        )
    }

    /**
     * Processes the input text by applying all configured transformations in sequence.
     *
     * @param text The input text to process.
     */
    private suspend fun processText(text: String) {
        // Apply all transformations in sequence
        var processedText = text
        for (transformation in transformations) {
            processedText = transformation(processedText)
        }

        // Get the output port and send the processed text
        val outputPort = getPort(OUTPUT_PORT, String::class)
        outputPort?.send(processedText)
    }

    /**
     * Starts the actor, initializing it if necessary.
     */
    override suspend fun start() {
        // Initialize the actor if it hasn't been initialized yet
        if (getPort(INPUT_PORT, String::class) == null) {
            initialize()
        }

        // Call the parent start method
        super.start()
    }
}
