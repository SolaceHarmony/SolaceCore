package ai.solace.core.actor.interfaces

import ai.solace.core.channels.Port
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull


@OptIn(ExperimentalCoroutinesApi::class)
class PortTest {

    @Test
    fun `test input port creation`() {
        val input = Port.Input("testInput", String::class)
        assertEquals("testInput", input.name)
        assertEquals(String::class, input.type)
        assertNull(input.channel)
    }

    @Test
    fun `test output port creation and connection`() = runTest {
        val output = Port.Output("testOutput", String::class)
        val channel = Channel<String>(capacity = 1)  // Ensure channel has buffer capacity

        output.connect(channel)

        // Send a test message
        output.send("test message")

        // Verify message was received
        val receivedMessage = channel.receive()
        assertEquals("test message", receivedMessage)

        // Close the channel to ensure all coroutines complete properly
        channel.close()
    }

    @Test
    fun `test tool port implementation and invocation`() = runTest {
        val tool = Port.Tool("testTool", String::class)

        tool.implement { input ->
            input.uppercase()
        }

        val result = tool.invoke("hello")
        assertEquals("HELLO", result)
    }

    @Test
    fun `test tool port throws when not implemented`() {
        val tool = Port.Tool("testTool", String::class)

        assertThrows<IllegalStateException> {
            runTest {
                tool.invoke("")
            }
        }
    }

    @Test
    fun `test output port with multiple connections`() = runTest {
        val output = Port.Output("multiOutput", String::class)
        val channel1 = Channel<String>(capacity = 1)
        val channel2 = Channel<String>(capacity = 1)

        output.connect(channel1)
        output.connect(channel2)

        output.send("test message")

        // Verify both channels received the message
        val receivedMessage1 = channel1.receive()
        val receivedMessage2 = channel2.receive()
        assertEquals("test message", receivedMessage1)
        assertEquals("test message", receivedMessage2)

        // Close the channels to avoid uncompleted coroutine issues
        channel1.close()
        channel2.close()
    }
}