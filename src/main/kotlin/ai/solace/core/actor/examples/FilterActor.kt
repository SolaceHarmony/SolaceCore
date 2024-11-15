package ai.solace.core.actor.examples

import ai.solace.core.actor.Actor
import kotlinx.coroutines.CoroutineScope

class FilterActor(scope: CoroutineScope) : Actor(scope = scope) {
    private val textInput = interface.input("text", String::class)
    private val filteredOutput = interface.output("filtered", String::class)
    private val filterTool = interface.tool("filter", String::class)
    
    override fun defineInterface() {
        filterTool.implement { text ->
            text.filter { it.isLetterOrDigit() }
        }
    }

    override suspend fun processMessage(message: ActorMessage) {
        when (message.type) {
            "FilterText" -> {
                val text = message.payload as String
                val filtered = filterTool.invoke(text) as String
                filteredOutput.send(filtered)
            }
            else -> handleError(
                IllegalArgumentException("Unknown message type: ${message.type}"),
                message
            )
        }
    }
}