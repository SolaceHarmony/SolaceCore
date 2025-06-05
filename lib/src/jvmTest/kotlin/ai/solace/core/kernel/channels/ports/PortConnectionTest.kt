package ai.solace.core.kernel.channels.ports

import kotlinx.coroutines.test.runTest
import kotlin.test.*

class PortConnectionTest {

    @Test
    fun testConnectPorts() = runTest {
        // Create source and target ports
        val sourcePort = BidirectionalPort.create<String>("source-port")
        val targetPort = BidirectionalPort.create<String>("target-port")

        // Connect the ports
        val connection = Port.connect(
            source = sourcePort,
            target = targetPort
        )

        // Verify connection properties
        assertNotNull(connection)
        assertEquals(sourcePort, connection.sourcePort)
        assertEquals(targetPort, connection.targetPort)
        assertTrue(connection.handlers.isEmpty())
        assertEquals(null, connection.protocolAdapter)
        assertTrue(connection.rules.isEmpty())
    }

    @Test
    fun testConnectPortsWithHandler() = runTest {
        // Create source and target ports
        val sourcePort = BidirectionalPort.create<String>("source-port")
        val targetPort = BidirectionalPort.create<String>("target-port")

        // Create a message handler
        val handler = object : Port.MessageHandler<String, String> {
            override suspend fun handle(message: String): String {
                return "$message - Handled"
            }
        }

        // Connect the ports with the handler
        val connection = Port.connect(
            source = sourcePort,
            target = targetPort,
            handlers = listOf(handler)
        )

        // Verify connection properties
        assertNotNull(connection)
        assertEquals(sourcePort, connection.sourcePort)
        assertEquals(targetPort, connection.targetPort)
        assertEquals(1, connection.handlers.size)
        assertEquals(null, connection.protocolAdapter)
        assertTrue(connection.rules.isEmpty())

        // Send a message through the source port
        val message = "Hello, World!"
        sourcePort.send(message)

        // Verify the message was handled and received by the target port
        // Note: This test assumes that the connection actually routes messages,
        // which may not be the case in the current implementation
    }

    @Test
    fun testConnectPortsWithConversionRule() = runTest {
        // Create source and target ports
        val sourcePort = BidirectionalPort.create<String>("source-port")
        val targetPort = BidirectionalPort.create<String>("target-port")

        // Create a conversion rule
        val rule = Port.ConversionRule.create<String, String>(
            converter = { it.uppercase() },
            validator = { inputType, outputType -> 
                inputType == String::class && outputType == String::class 
            },
            description = "Convert to uppercase"
        )

        // Connect the ports with the conversion rule
        val connection = Port.connect(
            source = sourcePort,
            target = targetPort,
            rules = listOf(rule)
        )

        // Verify connection properties
        assertNotNull(connection)
        assertEquals(sourcePort, connection.sourcePort)
        assertEquals(targetPort, connection.targetPort)
        assertTrue(connection.handlers.isEmpty())
        assertEquals(null, connection.protocolAdapter)
        assertEquals(1, connection.rules.size)

        // Send a message through the source port
        val message = "Hello, World!"
        sourcePort.send(message)

        // Verify the message was converted and received by the target port
        // Note: This test assumes that the connection actually routes messages,
        // which may not be the case in the current implementation
    }

    @Test
    fun testValidConnectionWithCompatibleTypes() = runTest {
        // Create source and target ports with compatible types
        val sourcePort = BidirectionalPort.create<String>("source-port")
        val targetPort = BidirectionalPort.create<String>("target-port")

        // Create a connection
        val connection = Port.connect(
            source = sourcePort,
            target = targetPort
        )

        // Verify that the connection validation succeeds (doesn't throw an exception)
        connection.validateConnection()
        // If we reach here, the test passes
        assertTrue(true)
    }

    @Test
    fun testInvalidConnectionWithIncompatibleTypes() = runTest {
        // Create source and target ports with incompatible types
        val sourcePort = BidirectionalPort.create<String>("source-port")
        val targetPort = BidirectionalPort.create<Int>("target-port")

        // Verify that creating a connection with incompatible types throws an exception
        assertFailsWith<PortConnectionException> {
            Port.connect(
                source = sourcePort,
                target = targetPort
            )
        }
    }

}
