package ai.solace.core.actor

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActorMessageTest {

    @Test
    fun testCreateActorMessage() = runTest {
        // Create a basic ActorMessage
        val payload = "Test payload"
        val message = ActorMessage(payload = payload)
        
        // Verify the message properties
        assertEquals(payload, message.payload)
        assertNotNull(message.correlationId)
        assertNull(message.sender)
        assertTrue(message.timestamp > 0)
        assertEquals(MessagePriority.NORMAL, message.priority)
        assertTrue(message.metadata.isEmpty())
    }
    
    @Test
    fun testCreateActorMessageWithAllParameters() = runTest {
        // Create an ActorMessage with all parameters
        val correlationId = "test-correlation-id"
        val payload = "Test payload"
        val sender = "test-sender"
        val timestamp = 1234567890L
        val priority = MessagePriority.HIGH
        val metadata = mapOf("key1" to "value1", "key2" to 42)
        
        val message = ActorMessage(
            correlationId = correlationId,
            payload = payload,
            sender = sender,
            timestamp = timestamp,
            priority = priority,
            metadata = metadata
        )
        
        // Verify the message properties
        assertEquals(correlationId, message.correlationId)
        assertEquals(payload, message.payload)
        assertEquals(sender, message.sender)
        assertEquals(timestamp, message.timestamp)
        assertEquals(priority, message.priority)
        assertEquals(metadata, message.metadata)
    }
    
    @Test
    fun testHighPriorityFactory() = runTest {
        // Create a high priority message using the factory method
        val payload = "Test payload"
        val sender = "test-sender"
        val message = ActorMessage.highPriority(payload, sender)
        
        // Verify the message properties
        assertEquals(payload, message.payload)
        assertEquals(sender, message.sender)
        assertEquals(MessagePriority.HIGH, message.priority)
        assertNotNull(message.correlationId)
        assertTrue(message.timestamp > 0)
        assertTrue(message.metadata.isEmpty())
    }
    
    @Test
    fun testWithMetadataFactory() = runTest {
        // Create a message with metadata using the factory method
        val payload = "Test payload"
        val metadata = mapOf("key1" to "value1", "key2" to 42)
        val message = ActorMessage.withMetadata(payload, metadata)
        
        // Verify the message properties
        assertEquals(payload, message.payload)
        assertEquals(metadata, message.metadata)
        assertEquals(MessagePriority.NORMAL, message.priority)
        assertNotNull(message.correlationId)
        assertTrue(message.timestamp > 0)
        assertNull(message.sender)
    }
    
    @Test
    fun testBetweenFactory() = runTest {
        // Create a message between actors using the factory method
        val payload = "Test payload"
        val sender = "test-sender"
        val metadata = mapOf("key1" to "value1", "key2" to 42)
        val message = ActorMessage.between(payload, sender, metadata)
        
        // Verify the message properties
        assertEquals(payload, message.payload)
        assertEquals(sender, message.sender)
        assertEquals(metadata, message.metadata)
        assertEquals(MessagePriority.NORMAL, message.priority)
        assertNotNull(message.correlationId)
        assertTrue(message.timestamp > 0)
    }
    
    @Test
    fun testActorMessageHandler() = runTest {
        // Create a test implementation of ActorMessageHandler
        val handler = object : ActorMessageHandler<String>() {
            override suspend fun processMessage(message: ActorMessage<String>): ActorMessage<String> {
                return ActorMessage(
                    correlationId = message.correlationId,
                    payload = message.payload.uppercase(),
                    sender = message.sender,
                    timestamp = message.timestamp,
                    priority = message.priority,
                    metadata = message.metadata
                )
            }
        }
        
        // Create a message to process
        val message = ActorMessage(payload = "test message")
        
        // Process the message using the handler
        val processedMessage = handler.handle(message)
        
        // Verify the processed message
        assertEquals(message.correlationId, processedMessage.correlationId)
        assertEquals("TEST MESSAGE", processedMessage.payload)
        assertEquals(message.sender, processedMessage.sender)
        assertEquals(message.timestamp, processedMessage.timestamp)
        assertEquals(message.priority, processedMessage.priority)
        assertEquals(message.metadata, processedMessage.metadata)
    }
    
    @Test
    fun testActorMessageWithComplexPayload() = runTest {
        // Define a complex data class for the payload
        data class ComplexPayload(val name: String, val value: Int, val nested: List<String>)
        
        // Create a message with a complex payload
        val payload = ComplexPayload("test", 42, listOf("a", "b", "c"))
        val message = ActorMessage(payload = payload)
        
        // Verify the message properties
        assertEquals(payload, message.payload)
        assertEquals("test", message.payload.name)
        assertEquals(42, message.payload.value)
        assertEquals(listOf("a", "b", "c"), message.payload.nested)
    }
}