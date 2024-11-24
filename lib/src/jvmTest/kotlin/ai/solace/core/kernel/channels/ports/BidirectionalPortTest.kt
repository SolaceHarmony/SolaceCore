package ai.solace.core.kernel.channels.ports

import ai.solace.core.kernel.channels.ports.Port.ConversionRule
import ai.solace.core.kernel.channels.ports.Port.MessageHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

@ExperimentalCoroutinesApi
class BidirectionalPortTest {

    @Test
    fun testSendAndReceive() = runBlocking {
        val port = BidirectionalPort<Int>(name = "testPort", type = Any::class)
        val message = 42

        port.send(message)
        val receivedMessage = port.receive()

        assertEquals(message, receivedMessage)
    }

    @Test
    fun testAddHandler() = runBlocking {
        val port = BidirectionalPort<Int>(name = "testPort", type = Any::class)
        val handler = object : MessageHandler<Int, Int> {
            override suspend fun handle(message: Int): Int {
                return message * 2
            }
        }
        port.addHandler(handler)
        val message = 21

        port.send(message)
        val receivedMessage = port.receive()

        assertEquals(message * 2, receivedMessage)
    }

    @Test
    fun testAddConversionRule() = runBlocking {
        val port = BidirectionalPort<Int>(name = "testPort", type = Any::class)
        val conversionRule = object : ConversionRule<Int, Int>() {
            override fun describe(): String {
                return "Integer increment conversion rule"
            }
            override suspend fun convert(input: Int): Int {
                return input + 1
            }

            override fun canHandle(inputType: Any, outputType: Any): Boolean {
                return true
            }
        }
        port.addConversionRule(conversionRule)
        val message = 41

        port.send(message)
        val receivedMessage = port.receive()

        assertEquals(message + 1, receivedMessage)
    }

    @Test
    fun testDispose() = runBlocking {
        val port = BidirectionalPort<Int>(name = "testPort", type = Any::class)
        port.dispose()

        // Attempting to send or receive should now fail
        var exceptionThrown = false
        try {
            port.send(42)
        } catch (_: Exception) {
            exceptionThrown = true
        }
        assert(exceptionThrown)
    }

    @Test
    fun testToString() {
        val port = BidirectionalPort<Int>(name = "testPort", type = Any::class)
        val expectedToString = "BidirectionalPort(id=${port.id}, name=${port.name}, type=${port.type})"

        assertEquals(expectedToString, port.toString())
    }
}