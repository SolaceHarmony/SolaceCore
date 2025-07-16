package ai.solace.core.actor.metrics

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class ActorMetricsTest {

    @Test
    fun testRecordMessageReceived() = runTest {
        // Create ActorMetrics instance
        val metrics = ActorMetrics()
        
        // Record a received message
        metrics.recordMessageReceived()
        
        // Get metrics and verify
        val allMetrics = metrics.getMetrics()
        assertEquals(1L, allMetrics["messagesReceived"])
        assertEquals(0L, allMetrics["messagesProcessed"])
        assertEquals(0L, allMetrics["messagesFailed"])
    }
    
    @Test
    fun testRecordMessageProcessed() = runTest {
        // Create ActorMetrics instance
        val metrics = ActorMetrics()
        
        // Record a processed message
        metrics.recordMessageProcessed()
        
        // Get metrics and verify
        val allMetrics = metrics.getMetrics()
        assertEquals(0L, allMetrics["messagesReceived"])
        assertEquals(1L, allMetrics["messagesProcessed"])
        assertEquals(0L, allMetrics["messagesFailed"])
    }
    
    @Test
    fun testRecordError() = runTest {
        // Create ActorMetrics instance
        val metrics = ActorMetrics()
        
        // Record an error
        metrics.recordError()
        
        // Get metrics and verify
        val allMetrics = metrics.getMetrics()
        assertEquals(0L, allMetrics["messagesReceived"])
        assertEquals(0L, allMetrics["messagesProcessed"])
        assertEquals(1L, allMetrics["messagesFailed"])
    }
    
    @Test
    fun testRecordProcessingTime() = runTest {
        // Create ActorMetrics instance
        val metrics = ActorMetrics()
        
        // Record processing time
        val duration = 100.milliseconds
        metrics.recordProcessingTime(duration)
        
        // Get metrics and verify
        val allMetrics = metrics.getMetrics()
        assertEquals(100L, allMetrics["lastProcessingTime"])
        assertEquals(100.0, allMetrics["averageProcessingTime"])
        assertEquals(100L, allMetrics["maxProcessingTime"])
        assertEquals(100L, allMetrics["minProcessingTime"])
    }
    
    @Test
    fun testRecordMultipleProcessingTimes() = runTest {
        // Create ActorMetrics instance
        val metrics = ActorMetrics()
        
        // Record multiple processing times
        metrics.recordProcessingTime(100.milliseconds)
        metrics.recordProcessingTime(200.milliseconds)
        metrics.recordProcessingTime(300.milliseconds)
        
        // Get metrics and verify
        val allMetrics = metrics.getMetrics()
        assertEquals(300L, allMetrics["lastProcessingTime"])
        assertEquals(200.0, allMetrics["averageProcessingTime"])
        assertEquals(300L, allMetrics["maxProcessingTime"])
        assertEquals(100L, allMetrics["minProcessingTime"])
    }
    
    @Test
    fun testRecordPriorityMessage() = runTest {
        // Create ActorMetrics instance
        val metrics = ActorMetrics()
        
        // Record priority messages
        metrics.recordPriorityMessage("HIGH")
        metrics.recordPriorityMessage("HIGH")
        metrics.recordPriorityMessage("NORMAL")
        
        // Get metrics and verify
        val allMetrics = metrics.getMetrics()
        val priorityMetrics = allMetrics["priorityMetrics"] as Map<*, *>
        assertEquals(2L, priorityMetrics["HIGH"])
        assertEquals(1L, priorityMetrics["NORMAL"])
    }
    
    @Test
    fun testRecordMessageWithProtocol() = runTest {
        // Create ActorMetrics instance
        val metrics = ActorMetrics()
        
        // Record messages with protocol
        metrics.recordMessageReceived("HTTP")
        metrics.recordMessageProcessed("HTTP")
        metrics.recordError("HTTP")
        metrics.recordMessageReceived("MQTT")
        
        // Get metrics and verify
        val allMetrics = metrics.getMetrics()
        val protocolMetrics = allMetrics["protocolMetrics"] as Map<*, *>
        assertEquals(1L, protocolMetrics["HTTP.received"])
        assertEquals(1L, protocolMetrics["HTTP.processed"])
        assertEquals(1L, protocolMetrics["HTTP.failed"])
        assertEquals(1L, protocolMetrics["MQTT.received"])
    }
    
    @Test
    fun testRecordMessageWithPort() = runTest {
        // Create ActorMetrics instance
        val metrics = ActorMetrics()
        
        // Record processing time with port
        metrics.recordProcessingTime(100.milliseconds, "input")
        metrics.recordProcessingTime(200.milliseconds, "output")
        metrics.recordProcessingTime(300.milliseconds, "input")
        
        // Get metrics and verify
        val allMetrics = metrics.getMetrics()
        val portMetrics = allMetrics["portMetrics"] as Map<*, *>
        assertEquals(2L, portMetrics["input"])
        assertEquals(1L, portMetrics["output"])
    }
    
    @Test
    fun testSuccessRate() = runTest {
        // Create ActorMetrics instance
        val metrics = ActorMetrics()
        
        // Record processed and failed messages
        metrics.recordMessageProcessed()
        metrics.recordMessageProcessed()
        metrics.recordMessageProcessed()
        metrics.recordError()
        
        // Get metrics and verify
        val allMetrics = metrics.getMetrics()
        assertEquals(75.0, allMetrics["successRate"])
    }
    
    @Test
    fun testReset() = runTest {
        // Create ActorMetrics instance
        val metrics = ActorMetrics()
        
        // Record various metrics
        metrics.recordMessageReceived()
        metrics.recordMessageProcessed()
        metrics.recordError()
        metrics.recordProcessingTime(100.milliseconds)
        metrics.recordPriorityMessage("HIGH")
        metrics.recordMessageReceived("HTTP")
        
        // Reset metrics
        metrics.reset()
        
        // Get metrics and verify they are reset
        val allMetrics = metrics.getMetrics()
        assertEquals(0L, allMetrics["messagesReceived"])
        assertEquals(0L, allMetrics["messagesProcessed"])
        assertEquals(0L, allMetrics["messagesFailed"])
        assertEquals(0L, allMetrics["lastProcessingTime"])
        
        // Verify collections are empty
        val protocolMetrics = allMetrics["protocolMetrics"] as Map<*, *>
        assertTrue(protocolMetrics.isEmpty())
        
        val priorityMetrics = allMetrics["priorityMetrics"] as Map<*, *>
        assertTrue(priorityMetrics.isEmpty())
        
        val portMetrics = allMetrics["portMetrics"] as Map<*, *>
        assertTrue(portMetrics.isEmpty())
    }
}