package ai.solace.core.kernel.channels.ports

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class BidirectionalPortTest {

    @Test
    fun testCreateBidirectionalPort() = runTest {
        // Create a bidirectional port
        val port = BidirectionalPort.create<String>("test-port")
        
        // Verify port properties
        assertEquals("test-port", port.name)
        assertNotNull(port.id)
        assertEquals(String::class, port.type)
    }
    
    @Test
    fun testSendAndReceiveMessage() = runTest {
        // Create a bidirectional port
        val port = BidirectionalPort.create<String>("test-port")
        
        // Send a message
        val message = "Hello, World!"
        port.send(message)
        
        // Receive the message
        val receivedMessage = port.receive()
        
        // Verify the message
        assertEquals(message, receivedMessage)
    }
    
    @Test
    fun testAddHandler() = runTest {
        // Create a bidirectional port
        val port = BidirectionalPort.create<String>("test-port")
        
        // Create a message handler that appends " - Handled"
        val handler = object : Port.MessageHandler<String, String> {
            override suspend fun handle(message: String): String {
                return "$message - Handled"
            }
        }
        
        // Add the handler
        port.addHandler(handler)
        
        // Send a message
        val message = "Hello, World!"
        port.send(message)
        
        // Receive the message
        val receivedMessage = port.receive()
        
        // Verify the message was handled
        assertEquals("$message - Handled", receivedMessage)
    }
    
    @Test
    fun testAddConversionRule() = runTest {
        // Create a bidirectional port
        val port = BidirectionalPort.create<String>("test-port")
        
        // Create a conversion rule that converts to uppercase
        val rule = Port.ConversionRule.create<String, String>(
            converter = { it.uppercase() },
            validator = { inputType, outputType -> 
                inputType == String::class && outputType == String::class 
            },
            description = "Convert to uppercase"
        )
        
        // Add the conversion rule
        port.addConversionRule(rule)
        
        // Send a message
        val message = "Hello, World!"
        port.send(message)
        
        // Receive the message
        val receivedMessage = port.receive()
        
        // Verify the message was converted
        assertEquals(message.uppercase(), receivedMessage)
    }
    
    @Test
    fun testHandlerAndConversionRuleChain() = runTest {
        // Create a bidirectional port
        val port = BidirectionalPort.create<String>("test-port")
        
        // Create a message handler that appends " - Handled"
        val handler = object : Port.MessageHandler<String, String> {
            override suspend fun handle(message: String): String {
                return "$message - Handled"
            }
        }
        
        // Create a conversion rule that converts to uppercase
        val rule = Port.ConversionRule.create<String, String>(
            converter = { it.uppercase() },
            validator = { inputType, outputType -> 
                inputType == String::class && outputType == String::class 
            },
            description = "Convert to uppercase"
        )
        
        // Add the handler and conversion rule
        port.addHandler(handler)
        port.addConversionRule(rule)
        
        // Send a message
        val message = "Hello, World!"
        port.send(message)
        
        // Receive the message
        val receivedMessage = port.receive()
        
        // Verify the message was handled and converted
        assertEquals("$message - Handled".uppercase(), receivedMessage)
    }
    
    @Test
    fun testDispose() = runTest {
        // Create a bidirectional port
        val port = BidirectionalPort.create<String>("test-port")
        
        // Dispose of the port
        port.dispose()
        
        // Verify that sending a message after disposing throws an exception
        assertFailsWith<PortException> {
            port.send("This should fail")
        }
    }
}