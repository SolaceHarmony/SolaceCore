package ai.solace.core.integration

import ai.solace.core.actor.Actor
import ai.solace.core.workflow.WorkflowManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.*

/**
 * Integration tests for port-to-port communication across actors in workflows.
 * 
 * These tests validate that messages flow correctly between connected actors
 * and that the entire communication pipeline works end-to-end.
 */
class PortCommunicationIntegrationTest {

    /**
     * A simple producer actor that sends messages on its output port.
     */
    private class ProducerActor(id: String = "producer") : Actor(id = id, name = "Producer") {
        private var outputPort: ai.solace.core.kernel.channels.ports.Port<String>? = null
        
        suspend fun createOutputPort() {
            outputPort = createPort(
                name = "output",
                messageClass = String::class,
                handler = { /* Output port - no incoming handling needed */ },
                bufferSize = 10
            )
        }
        
        suspend fun sendMessage(message: String) {
            outputPort?.send(message) ?: throw IllegalStateException("Output port not created")
        }
        
        fun getOutputPort() = outputPort
    }

    /**
     * A simple consumer actor that receives messages on its input port.
     */
    private class ConsumerActor(id: String = "consumer") : Actor(id = id, name = "Consumer") {
        private var inputPort: ai.solace.core.kernel.channels.ports.Port<String>? = null
        private val receivedMessages = mutableListOf<String>()
        
        suspend fun createInputPort() {
            inputPort = createPort(
                name = "input",
                messageClass = String::class,
                handler = { message ->
                    receivedMessages.add(message)
                },
                bufferSize = 10
            )
        }
        
        fun getInputPort() = inputPort
        fun getReceivedMessages(): List<String> = receivedMessages.toList()
        fun clearReceivedMessages() = receivedMessages.clear()
    }

    /**
     * A transformer actor that receives messages, transforms them, and sends them out.
     */
    private class TransformerActor(
        id: String = "transformer",
        private val transformation: (String) -> String = { it.uppercase() }
    ) : Actor(id = id, name = "Transformer") {
        private var inputPort: ai.solace.core.kernel.channels.ports.Port<String>? = null
        private var outputPort: ai.solace.core.kernel.channels.ports.Port<String>? = null
        
        suspend fun createPorts() {
            inputPort = createPort(
                name = "input",
                messageClass = String::class,
                handler = { message ->
                    val transformed = transformation(message)
                    outputPort?.send(transformed)
                },
                bufferSize = 10
            )
            
            outputPort = createPort(
                name = "output",
                messageClass = String::class,
                handler = { /* Output port - no incoming handling needed */ },
                bufferSize = 10
            )
        }
        
        fun getInputPort() = inputPort
        fun getOutputPort() = outputPort
    }

    @Test
    fun testSimplePortToPortCommunication() = runTest {
        // Create workflow manager
        val workflow = WorkflowManager(name = "SimpleCommTest")
        
        // Create producer and consumer actors
        val producer = ProducerActor()
        val consumer = ConsumerActor()
        
        // Create ports
        producer.createOutputPort()
        consumer.createInputPort()
        
        // Add actors to workflow
        workflow.addActor(producer)
        workflow.addActor(consumer)
        
        // Connect producer output to consumer input
        workflow.connectActors(producer, "output", consumer, "input")
        
        // Start the workflow
        workflow.start()
        
        // Wait a moment for initialization
        delay(100)
        
        // Send test messages
        val testMessages = listOf("Hello", "World", "Test", "Message")
        for (message in testMessages) {
            producer.sendMessage(message)
            delay(50) // Allow time for message processing
        }
        
        // Verify messages were received
        withTimeout(5000) {
            while (consumer.getReceivedMessages().size < testMessages.size) {
                delay(10)
            }
        }
        
        val receivedMessages = consumer.getReceivedMessages()
        assertEquals(testMessages.size, receivedMessages.size)
        assertEquals(testMessages, receivedMessages)
        
        // Clean up
        workflow.stop()
        workflow.dispose()
    }

    @Test
    fun testChainedPortCommunication() = runTest {
        // Create workflow manager
        val workflow = WorkflowManager(name = "ChainedCommTest")
        
        // Create producer, transformer, and consumer actors
        val producer = ProducerActor()
        val transformer = TransformerActor()
        val consumer = ConsumerActor()
        
        // Create ports
        producer.createOutputPort()
        transformer.createPorts()
        consumer.createInputPort()
        
        // Add actors to workflow
        workflow.addActor(producer)
        workflow.addActor(transformer)
        workflow.addActor(consumer)
        
        // Connect in chain: producer -> transformer -> consumer
        workflow.connectActors(producer, "output", transformer, "input")
        workflow.connectActors(transformer, "output", consumer, "input")
        
        // Start the workflow
        workflow.start()
        
        // Wait a moment for initialization
        delay(100)
        
        // Send test messages
        val testMessages = listOf("hello", "world", "test")
        for (message in testMessages) {
            producer.sendMessage(message)
            delay(50) // Allow time for message processing
        }
        
        // Verify transformed messages were received
        withTimeout(5000) {
            while (consumer.getReceivedMessages().size < testMessages.size) {
                delay(10)
            }
        }
        
        val receivedMessages = consumer.getReceivedMessages()
        val expectedMessages = testMessages.map { it.uppercase() }
        
        assertEquals(expectedMessages.size, receivedMessages.size)
        assertEquals(expectedMessages, receivedMessages)
        
        // Clean up
        workflow.stop()
        workflow.dispose()
    }

    @Test
    fun testWorkflowLifecycleWithConnections() = runTest {
        // Create workflow manager
        val workflow = WorkflowManager(name = "LifecycleTest")
        
        // Create actors
        val producer = ProducerActor()
        val consumer = ConsumerActor()
        
        // Create ports
        producer.createOutputPort()
        consumer.createInputPort()
        
        // Add actors and connect them
        workflow.addActor(producer)
        workflow.addActor(consumer)
        workflow.connectActors(producer, "output", consumer, "input")
        
        // Test multiple start/stop cycles
        for (cycle in 1..3) {
            workflow.start()
            delay(50)
            
            producer.sendMessage("cycle$cycle")
            delay(50)
            
            workflow.stop()
            delay(50)
        }
        
        // Final start and test
        workflow.start()
        delay(50)
        
        producer.sendMessage("final")
        // Wait until the final message is observed to avoid timing flakiness
        withTimeout(2000) {
            while (!consumer.getReceivedMessages().contains("final")) {
                delay(10)
            }
        }
        
        // Clean up
        workflow.stop()
        workflow.dispose()
    }
}
