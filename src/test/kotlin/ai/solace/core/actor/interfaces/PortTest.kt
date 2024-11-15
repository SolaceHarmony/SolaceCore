package ai.solace.core.actor.interfaces

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
        val channel = Channel<String>()
        
        output.connect(channel)
        
        // Send a test message
        output.send("test message")
        
        // Verify message was received
        assertEquals("test message", channel.receive())
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
                tool.invoke("test")
            }
        }
    }

    @Test
    fun `test output port with multiple connections`() = runTest {
        val output = Port.Output("multiOutput", String::class)
        val channel1 = Channel<String>()
        val channel2 = Channel<String>()
        
        output.connect(channel1)
        output.connect(channel2)
        
        output.send("test message")
        
        // Verify both channels received the message
        assertEquals("test message", channel1.receive())
        assertEquals("test message", channel2.receive())
    }
}