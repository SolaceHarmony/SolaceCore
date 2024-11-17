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
        val result = output.send("test message")
        assertTrue(result.isSuccess)
        assertEquals(Result.success("test message"), input.receive())
    }

    @Test
    fun `test port connection with incompatible types throws`() {
        val actorInterface = ActorInterface()

        val output = actorInterface.output("out", String::class)
        val input = actorInterface.input("in", Int::class) as Port.Input<String> // Force type mismatch for the test

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
        assertTrue(result.isSuccess)
        assertEquals(Result.success("HELLO"), result)
    }

    @Test
    fun `test saveState serializes actor state`() {
        val actorInterface = ActorInterface()

        val input = actorInterface.input("testInput", String::class)
        val output = actorInterface.output("testOutput", String::class)
        val tool = actorInterface.tool("testTool", String::class)

        val stateJson = actorInterface.saveState()

        // Assert that JSON contains expected data
        assertTrue(stateJson.contains("testInput"))
        assertTrue(stateJson.contains("testOutput"))
        assertTrue(stateJson.contains("testTool"))
    }

    @Test
    fun `test saveState handles serialization error`() {
        val actorInterface = ActorInterface()

        // Mock an error scenario if necessary

        assertThrows<RuntimeException> {
            actorInterface.saveState()
        }
    }

    @Test
    fun `test restoreState deserializes actor state`() {
        val actorInterface = ActorInterface()

        val stateJson = """
            {
                "inputs": {"testInput": {"name": "testInput", "type": "kotlin.String"}},
                "outputs": {"testOutput": {"name": "testOutput", "type": "kotlin.String"}},
                "tools": {"testTool": {"name": "testTool", "type": "kotlin.String"}}
            }
        """.trimIndent()

        actorInterface.restoreState(stateJson)

        // Assertions to validate the state was restored correctly
        assertNotNull(actorInterface.getInput("testInput"))
        assertNotNull(actorInterface.getOutput("testOutput"))
        assertNotNull(actorInterface.getTool("testTool"))
    }
}