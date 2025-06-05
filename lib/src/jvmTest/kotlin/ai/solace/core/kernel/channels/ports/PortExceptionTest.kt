package ai.solace.core.kernel.channels.ports

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class PortExceptionTest {

    @Test
    fun testPortConnectionExceptionMessage() = runTest {
        // Create source and target ports with incompatible types
        val sourcePort = BidirectionalPort.create<String>("source-port")
        val targetPort = BidirectionalPort.create<Int>("target-port")
        
        // Attempt to connect the ports and catch the exception
        val exception = assertFailsWith<PortConnectionException> {
            Port.connect(
                source = sourcePort,
                target = targetPort
            )
        }
        
        // Verify the exception message contains the expected information
        val expectedMessageParts = listOf(
            "Failed to connect port",
            sourcePort.id,
            "to",
            targetPort.id,
            "Incompatible port types",
            "String",
            "Int"
        )
        
        expectedMessageParts.forEach { part ->
            assertTrue(exception.message?.contains(part) == true, 
                "Exception message should contain '$part', but was: ${exception.message}")
        }
    }
    
    @Test
    fun testSendMessageExceptionWhenPortIsClosed() = runTest {
        // Create a port
        val port = BidirectionalPort.create<String>("test-port")
        
        // Close the port
        port.dispose()
        
        // Attempt to send a message to the closed port and catch the exception
        val exception = assertFailsWith<PortException> {
            port.send("Test message")
        }
        
        // Verify the exception message contains the expected information
        assertTrue(exception.message?.contains("Failed to send message") == true,
            "Exception message should indicate a send failure, but was: ${exception.message}")
    }
    
    @Test
    fun testPortExceptionValidationWhenHandlerFails() = runTest {
        // Create a port
        val port = BidirectionalPort.create<String>("test-port")
        
        // Add a handler that throws an exception
        val handler = object : Port.MessageHandler<String, String> {
            override suspend fun handle(message: String): String {
                throw IllegalArgumentException("Test handler exception")
            }
        }
        port.addHandler(handler)
        
        // Attempt to send a message and catch the exception
        val exception = assertFailsWith<PortException> {
            port.send("Test message")
        }
        
        // Verify the exception is a PortException.Validation and contains the expected information
        assertTrue(exception.message?.contains("Failed to send message") == true,
            "Exception message should indicate a send failure, but was: ${exception.message}")
        assertTrue(exception.message?.contains("Test handler exception") == true,
            "Exception message should contain the handler exception message, but was: ${exception.message}")
    }
    
    @Test
    fun testPortExceptionValidationWhenConversionRuleFails() = runTest {
        // Create a port
        val port = BidirectionalPort.create<String>("test-port")
        
        // Add a conversion rule that throws an exception
        val rule = Port.ConversionRule.create<String, String>(
            converter = { throw IllegalArgumentException("Test conversion exception") },
            description = "Test conversion rule"
        )
        port.addConversionRule(rule)
        
        // Attempt to send a message and catch the exception
        val exception = assertFailsWith<PortException> {
            port.send("Test message")
        }
        
        // Verify the exception is a PortException.Validation and contains the expected information
        assertTrue(exception.message?.contains("Failed to send message") == true,
            "Exception message should indicate a send failure, but was: ${exception.message}")
        assertTrue(exception.message?.contains("Test conversion exception") == true,
            "Exception message should contain the conversion exception message, but was: ${exception.message}")
    }
}