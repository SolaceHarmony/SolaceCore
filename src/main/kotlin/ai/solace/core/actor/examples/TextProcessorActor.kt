package ai.solace.core.actor.examples

import ai.solace.core.actor.Actor
import kotlinx.coroutines.CoroutineScope

class TextProcessorActor(scope: CoroutineScope) : Actor(scope = scope) {
    private val textInput = actorInterface.input("text", String::class)
    private val processedOutput = actorInterface.output("processed", String::class)
    private val upperCaseTool = actorInterface.tool("upperCase", String::class)
    
    override fun defineInterface() {
        upperCaseTool.implement { text ->
            text.uppercase()
        }
    }

    override suspend fun processMessage(message: ActorMessage) {
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