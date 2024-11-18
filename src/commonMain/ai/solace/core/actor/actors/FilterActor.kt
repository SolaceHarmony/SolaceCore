package ai.solace.core.actor.actors

import ai.solace.core.actor.Actor
import kotlinx.coroutines.CoroutineScope

/**
 * FilterActor is an abstract class that extends the Actor class, specifically designed to filter
 * strings by removing non-letter and non-digit characters.
 *
 * This actor defines a filter tool and a filtered output. The tool is implemented to filter
 * characters in a provided text, and the filtered text is sent to the filtered output.
 *
 * @param scope The coroutine scope in which the actor runs.
 */
abstract class FilterActor(scope: CoroutineScope) : Actor("FilterActor", scope) {

    /**
     * The `actorInterface` property holds the `ActorInterface` for the actor,
     * providing access to the actor's input ports, output ports, and tools.
     * This interface is used to interact with the actor's defined components
     * and is essential for configuring its behavior and communication channels.
     *
     * In the context of the containing actor class, `actorInterface` is typically
     * initialized through the `getInterface` method.
     */
    override val actorInterface = getInterface()
    /**
     * Output port used to send filtered text data from the actor.
     * This port accepts strings that have been processed and filtered,
     * typically to remove non-letter or non-digit characters by the filterTool.
     * It is part of the actor's output interface.
     */
    private val filteredOutput = actorInterface.output("filtered", String::class)
    /**
     * A tool within the `FilterActor` used to filter out non-alphanumeric characters from input strings.
     *
     * This tool is defined and implemented in the `defineInterface` method where it filters
     * the input text to include only letters and digits.
     *
     * Usage within the actor's lifecycle:
     * - In the `defineInterface` method, this tool is implemented to filter text by removing non-alphanumeric characters.
     * - In the `processMessage` method, the tool is invoked to process and filter incoming text messages.
     */
    private val filterTool = actorInterface.tool("filter", String::class)

    /**
     * Defines the actor's interface by setting up the filter tool.
     * This method configures the `filterTool` to filter text, removing characters
     * that are not letters or digits.
     * The implementation is provided as a lambda function which receives the text
     * and returns only the characters that are letters or digits.
     */
    override fun defineInterface() {
        filterTool.implement { text ->
            text.filter { it.isLetterOrDigit() }
        }
    }

    /**
     * Processes an incoming actor message by determining its type and acting accordingly.
     *
     * @param message The incoming message to be processed, of type `ActorBase.ActorMessage`.
     *                - If the message type is "FilterText", it filters the text payload.
     *                  The filtering process removes any characters that are not letters or digits.
     *                - If the message type is unknown, it calls `handleError` to manage the error.
     */
    override suspend fun processMessage(message: ActorMessage) {
        when (message.type) {
            "FilterText" -> {
                val text = message.payload as String
                val filtered = filterTool.invoke(text)
                filteredOutput.send(filtered)
            }
            else -> handleError(
                IllegalArgumentException("Unknown message type: ${message.type}"),
                message
            )
        }
    }
}
