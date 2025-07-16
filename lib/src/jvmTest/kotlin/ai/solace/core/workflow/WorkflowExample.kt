package ai.solace.core.workflow

import ai.solace.core.actor.builder.buildActorNetwork
import ai.solace.core.actor.examples.Filter
import ai.solace.core.actor.examples.TextProcessor
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

/**
 * Example demonstrating the use of the workflow management system.
 */
class WorkflowExample {

    /**
     * Demonstrates a simple text processing workflow.
     *
     * The workflow consists of:
     * 1. A TextProcessor that converts text to uppercase
     * 2. A Filter that only accepts strings longer than 5 characters
     * 3. Another TextProcessor that removes whitespace
     */
    @Test
    fun textProcessingWorkflowExample() = runBlocking {
        // Create the actors
        val uppercaseProcessor = TextProcessor(
            name = "UppercaseProcessor",
            transformations = listOf(TextProcessor.TO_UPPERCASE)
        )
        
        val lengthFilter = Filter(
            name = "LengthFilter",
            predicate = { text: String -> text.length > 5 },
            messageClass = String::class,
            includeRejectedPort = true
        )
        
        val whitespaceRemover = TextProcessor(
            name = "WhitespaceRemover",
            transformations = listOf(TextProcessor.REMOVE_WHITESPACE)
        )
        
        // Initialize the actors
        uppercaseProcessor.initialize()
        lengthFilter.initialize()
        whitespaceRemover.initialize()
        
        // Build the workflow using the ActorBuilder
        val workflow = buildActorNetwork()
            .addActor(uppercaseProcessor, mapOf(
                TextProcessor.INPUT_PORT to String::class,
                TextProcessor.OUTPUT_PORT to String::class
            ))
            .addActor(lengthFilter, mapOf(
                Filter.INPUT_PORT to String::class,
                Filter.ACCEPTED_PORT to String::class,
                Filter.REJECTED_PORT to String::class
            ))
            .addActor(whitespaceRemover, mapOf(
                TextProcessor.INPUT_PORT to String::class,
                TextProcessor.OUTPUT_PORT to String::class
            ))
            .connect(
                uppercaseProcessor, TextProcessor.OUTPUT_PORT,
                lengthFilter, Filter.INPUT_PORT
            )
            .connect(
                lengthFilter, Filter.ACCEPTED_PORT,
                whitespaceRemover, TextProcessor.INPUT_PORT
            )
            .build()
        
        // Start the workflow
        workflow.start()
        
        // Create a test harness to capture the output
        val testResults = mutableListOf<String>()
        
        // Create a port to capture the output from the whitespace remover
        val outputPort = whitespaceRemover.getPort(TextProcessor.OUTPUT_PORT, String::class)
        
        // Process some test inputs
        val inputPort = uppercaseProcessor.getPort(TextProcessor.INPUT_PORT, String::class)
        
        // Send test inputs
        inputPort?.send("Hello World")  // Should be processed: "HELLOWORLD"
        inputPort?.send("Hi")           // Should be filtered out (too short)
        inputPort?.send("Welcome to Solace Core")  // Should be processed: "WELCOMETOSOLACECORE"
        
        // Wait for processing to complete (in a real application, you would use a more robust approach)
        kotlinx.coroutines.delay(100)
        
        // Stop the workflow
        workflow.stop()
        
        // In a real application, you would connect the output port to another actor or system
        // For this example, we're just demonstrating the workflow structure
        
        // Dispose the workflow
        workflow.dispose()
    }

    /**
     * Demonstrates how to use the WorkflowManager directly.
     */
    @Test
    fun workflowManagerDirectUsageExample() = runBlocking {
        // Create a workflow manager
        val workflowManager = WorkflowManager(name = "DirectWorkflowExample")
        
        // Create actors
        val textProcessor = TextProcessor(
            name = "TextProcessor",
            transformations = listOf(TextProcessor.TO_UPPERCASE, TextProcessor.TRIM)
        )
        
        val filter = Filter(
            name = "Filter",
            predicate = { text: String -> text.contains("SOLACE") },
            messageClass = String::class
        )
        
        // Initialize actors
        textProcessor.initialize()
        filter.initialize()
        
        // Add actors to the workflow
        workflowManager.addActor(textProcessor)
        workflowManager.addActor(filter)
        
        // Connect actors
        workflowManager.connectActors(
            textProcessor, TextProcessor.OUTPUT_PORT,
            filter, Filter.INPUT_PORT
        )
        
        // Start the workflow
        workflowManager.start()
        
        // Use the workflow
        val inputPort = textProcessor.getPort(TextProcessor.INPUT_PORT, String::class)
        inputPort?.send("  solace core framework  ")
        
        // Wait for processing to complete
        kotlinx.coroutines.delay(100)
        
        // Pause the workflow
        workflowManager.pause("Example paused for demonstration")
        
        // Resume the workflow
        workflowManager.resume()
        
        // Stop the workflow
        workflowManager.stop()
        
        // Dispose the workflow
        workflowManager.dispose()
    }
}