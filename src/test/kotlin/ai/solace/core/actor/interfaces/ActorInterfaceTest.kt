package ai.solace.core.actor.interfaces

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ActorInterfaceTest {

    @Test
    fun `test port creation and retrieval`() {
        val actorInterface = ActorInterface()
        
        val input = actorInterface.input("testInput", String::class)
        val output = actorInterface.output("testOutput", String::class)
        val tool = actorInterface.tool("testTool", String::class)
        
        assertEquals(input, actorInterface.getInput("testInput"))
        assertEquals(output, actorInterface.getOutput("testOutput"))
        assertEquals(tool, actorInterface.getTool("testTool"))
    }

    @Test
    fun `test port connection with compatible types`() = runTest {
        val actorInterface = ActorInterface()
        
        val output = actorInterface.output("out", String::class)
        val input = actorInterface.input("in", String::class)
        
        actorInterface.connect(output, input)
        
        // Test the connection works
        output.send("test message")
        assertEquals("test message", input.receive())
    }

    @Test
    fun `test port connection with incompatible types throws`() {
        val actorInterface = ActorInterface()
        
        val output = actorInterface.output("out", String::class)
        val input = actorInterface.input("in", Int::class)
        
        assertThrows<IllegalArgumentException> {
            actorInterface.connect(output, input)
        }
    }

    @Test
    fun `test getAllPorts returns all registered ports`() {
        val actorInterface = ActorInterface()
        
        actorInterface.input("in1", String::class)
        actorInterface.input("in2", Int::class)
        actorInterface.output("out1", String::class)
        actorInterface.tool("tool1", Boolean::class)
        
        val ports = actorInterface.getAllPorts()
        
        assertEquals(2, ports["inputs"]?.size)
        assertEquals(1, ports["outputs"]?.size)
        assertEquals(1, ports["tools"]?.size)
    }

    @Test
    fun `test tool implementation and execution`() = runTest {
        val actorInterface = ActorInterface()
        
        val tool = actorInterface.tool("uppercase", String::class)
        tool.implement { text -> text.uppercase() }
        
        val result = tool.invoke("hello")
        assertEquals("HELLO", result)
    }
}