package ai.solace.core.examples

import ai.solace.core.actor.examples.FilterActor
import ai.solace.core.actor.examples.TextProcessorActor
import ai.solace.core.workflow.WorkflowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val scope = CoroutineScope(Dispatchers.Default)
    
    // Create actors
    val textProcessor = TextProcessorActor(scope)
    val filter = FilterActor(scope)
    
    // Build workflow
    val workflow = WorkflowManager.builder(scope)
        .addActor(textProcessor)
        .addActor(filter)
        .connect(
            textProcessor,
            "processed",
            filter,
            "text",
            String::class
        )
        .build()
    
    // Start workflow
    workflow.start()
    
    // Send a test message
    scope.launch {
        textProcessor.send(ai.solace.core.actor.Actor.ActorMessage(
            type = "ProcessText",
            payload = "Hello, World! 123"
        ))
    }
    
    // Wait for a bit to see the results
    kotlinx.coroutines.delay(1000)
    
    // Stop workflow
    workflow.stop()
}