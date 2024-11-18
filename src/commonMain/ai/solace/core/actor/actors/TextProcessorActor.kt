package ai.solace.core.actor.actors

import ai.solace.core.actor.Actor
import ai.solace.core.actor.ActorBase
import kotlinx.coroutines.CoroutineScope

/**
 * An actor class designed for text processing. This actor converts text to uppercase
 * and sends the processed text to an output port.
 *
 * @param scope The CoroutineScope within which the actor operates.
 */
abstract class TextProcessorActor(scope: CoroutineScope) : Actor("TextProcessorActor", scope) {
    /**
     * Output port used to send processed text data from the actor.
     * This port accepts strings that have been processed,
     * typically transformed to uppercase by the upperCaseTool.
     * It is part of the actor's output interface.
     */
    private val processedOutput = actorInterface.output("processed", String::class)
    /**
     * A tool within the `TextProcessorActor` that converts strings to uppercase.
     *
     * This tool is used to process text messages by transforming the given string payload to uppercase.
     * It is defined within the `defineInterface` method, where its implementation specifies the uppercase transformation.
     *
     * Usage within the actor's lifecycle:
     * - In the `defineInterface` method, this tool is implemented to convert input text to uppercase.
     * - In the `processMessage` method, the tool is invoked to process incoming text messages.
     */
    private val upperCaseTool = actorInterface.tool("upperCase", String::class)

    /**
     * Defines the actor's interface by setting up a tool for text conversion.
     * This method configures the `upperCaseTool` to convert text to uppercase.
     * The implementation is provided as a lambda function which receives the text
     * and returns its uppercase representation.
     */
    override fun defineInterface() {
        upperCaseTool.implement { text ->
            text.uppercase()
        }
    }

    /**
     * Processes an incoming actor message by determining its type and acting accordingly.
     *
     * @param message The incoming message to be processed, of type `ActorBase.ActorMessage`.
     */
    override suspend fun processMessage(message: ActorBase.ActorMessage) {
        when (message.type) {
            "ProcessText" -> {
                val text = message.payload as String
                val processed = upperCaseTool.invoke(text) as String
                processedOutput.send(processed)
            }
            else -> handleError(
                IllegalArgumentException("Unknown message type: ${message.type}"),
                message
            )
        }
    }
}