package ai.solace.core.actor

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertNotEquals

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
    
    // ===== ENHANCED EDGE CASE TESTS =====
    
    @Test
    fun testActorMessageWithEmptyMetadata() = runTest {
        val payload = "test"
        val message = ActorMessage(payload = payload, metadata = emptyMap())
        
        assertEquals(payload, message.payload)
        assertTrue(message.metadata.isEmpty())
        assertEquals(MessagePriority.NORMAL, message.priority)
    }
    
    @Test
    fun testActorMessageWithLargeMetadata() = runTest {
        val payload = "test"
        val largeMetadata = (1..100).associate { "key$it" to "value$it" }
        val message = ActorMessage(payload = payload, metadata = largeMetadata)
        
        assertEquals(payload, message.payload)
        assertEquals(100, message.metadata.size)
        assertEquals("value50", message.metadata["key50"])
    }
    
    @Test
    fun testActorMessageCorrelationIdUniqueness() = runTest {
        val message1 = ActorMessage(payload = "test1")
        val message2 = ActorMessage(payload = "test2")
        
        // Correlation IDs should be unique
        assertNotEquals(message1.correlationId, message2.correlationId)
        assertNotNull(message1.correlationId)
        assertNotNull(message2.correlationId)
        assertTrue(message1.correlationId.isNotEmpty())
        assertTrue(message2.correlationId.isNotEmpty())
    }
    
    @Test
    fun testActorMessageTimestampProgression() = runTest {
        val message1 = ActorMessage(payload = "test1")
        delay(10) // Small delay to ensure different timestamps
        val message2 = ActorMessage(payload = "test2")
        
        // Second message should have later timestamp
        assertTrue(message2.timestamp >= message1.timestamp)
    }
    
    @Test
    fun testHighPriorityFactoryWithoutSender() = runTest {
        val payload = "test"
        val message = ActorMessage.highPriority(payload)
        
        assertEquals(payload, message.payload)
        assertEquals(MessagePriority.HIGH, message.priority)
        assertNull(message.sender)
        assertTrue(message.metadata.isEmpty())
    }
    
    @Test
    fun testBetweenFactoryWithEmptyMetadata() = runTest {
        val payload = "test"
        val sender = "sender-actor"
        val message = ActorMessage.between(payload, sender)
        
        assertEquals(payload, message.payload)
        assertEquals(sender, message.sender)
        assertTrue(message.metadata.isEmpty())
        assertEquals(MessagePriority.NORMAL, message.priority)
    }
    
    // ===== COMPLEX PAYLOAD TYPE TESTS =====
    
    @Test
    fun testActorMessageWithCollectionPayload() = runTest {
        val payload = listOf("item1", "item2", "item3")
        val message = ActorMessage(payload = payload)
        
        assertEquals(payload, message.payload)
        assertEquals(3, message.payload.size)
        assertEquals("item2", message.payload[1])
    }
    
    @Test
    fun testActorMessageWithMapPayload() = runTest {
        val payload = mapOf("key1" to 1, "key2" to 2, "key3" to 3)
        val message = ActorMessage(payload = payload)
        
        assertEquals(payload, message.payload)
        assertEquals(3, message.payload.size)
        assertEquals(2, message.payload["key2"])
    }
    
    @Test
    fun testActorMessageWithNestedComplexPayload() = runTest {
        data class InnerData(val id: Int, val name: String)
        data class OuterData(val inner: InnerData, val tags: List<String>)
        
        val payload = OuterData(
            inner = InnerData(123, "test"),
            tags = listOf("tag1", "tag2")
        )
        val message = ActorMessage(payload = payload)
        
        assertEquals(payload, message.payload)
        assertEquals(123, message.payload.inner.id)
        assertEquals("test", message.payload.inner.name)
        assertEquals(2, message.payload.tags.size)
        assertEquals("tag1", message.payload.tags[0])
    }
    
    // ===== MESSAGE PRIORITY TESTS =====
    
    @Test
    fun testAllMessagePriorityLevels() = runTest {
        val highMessage = ActorMessage(payload = "test", priority = MessagePriority.HIGH)
        val normalMessage = ActorMessage(payload = "test", priority = MessagePriority.NORMAL)
        val lowMessage = ActorMessage(payload = "test", priority = MessagePriority.LOW)
        
        assertEquals(MessagePriority.HIGH, highMessage.priority)
        assertEquals(MessagePriority.NORMAL, normalMessage.priority)
        assertEquals(MessagePriority.LOW, lowMessage.priority)
    }
    
    // ===== ACTOR MESSAGE HANDLER EDGE CASES =====
    
    @Test
    fun testActorMessageHandlerWithComplexTransformation() = runTest {
        val handler = object : ActorMessageHandler<String>() {
            override suspend fun processMessage(message: ActorMessage<String>): ActorMessage<String> {
                // Complex transformation: reverse string and add metadata
                val processedPayload = message.payload.reversed()
                val newMetadata = message.metadata + ("processed" to true) + ("originalLength" to message.payload.length)
                
                return ActorMessage(
                    correlationId = message.correlationId,
                    payload = processedPayload,
                    sender = message.sender,
                    timestamp = message.timestamp,
                    priority = MessagePriority.HIGH, // Upgrade priority
                    metadata = newMetadata
                )
            }
        }
        
        val originalMessage = ActorMessage(
            payload = "hello",
            metadata = mapOf("original" to true)
        )
        
        val processedMessage = handler.handle(originalMessage)
        
        assertEquals("olleh", processedMessage.payload)
        assertEquals(MessagePriority.HIGH, processedMessage.priority)
        assertTrue(processedMessage.metadata["processed"] as Boolean)
        assertEquals(5, processedMessage.metadata["originalLength"])
        assertTrue(processedMessage.metadata["original"] as Boolean)
    }
    
    @Test
    fun testActorMessageHandlerPreservesCorrelationId() = runTest {
        val handler = object : ActorMessageHandler<Int>() {
            override suspend fun processMessage(message: ActorMessage<Int>): ActorMessage<Int> {
                return ActorMessage(
                    correlationId = message.correlationId, // Preserve correlation ID
                    payload = message.payload * 2,
                    sender = message.sender,
                    timestamp = System.currentTimeMillis(), // New timestamp
                    priority = message.priority,
                    metadata = message.metadata
                )
            }
        }
        
        val originalMessage = ActorMessage(payload = 21)
        val processedMessage = handler.handle(originalMessage)
        
        assertEquals(originalMessage.correlationId, processedMessage.correlationId)
        assertEquals(42, processedMessage.payload)
        assertTrue(processedMessage.timestamp >= originalMessage.timestamp)
    }
}