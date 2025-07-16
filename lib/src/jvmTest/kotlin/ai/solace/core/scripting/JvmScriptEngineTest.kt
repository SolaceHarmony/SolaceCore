package ai.solace.core.scripting

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JvmScriptEngineTest {

    @Test
    fun testScriptCompilation() = runBlocking {
        // Create a script engine
        val scriptEngine = JvmScriptEngine()

        // Define a simple script
        val scriptSource = """
            val result = "Hello, World!"
            result
        """.trimIndent()

        // Compile the script
        val compiledScript = scriptEngine.compile(scriptSource, "test-script")

        // Verify the compiled script
        assertNotNull(compiledScript)
        assertEquals("test-script", compiledScript.name)
    }

    @Test
    fun testScriptExecution() = runBlocking {
        // Create a script engine
        val scriptEngine = JvmScriptEngine()

        // Define a simple script
        val scriptSource = """
            val a = 10
            val b = 20
            val sum = a + b
            sum
        """.trimIndent()

        // Compile the script
        val compiledScript = scriptEngine.compile(scriptSource, "math-script")

        // Execute the script
        val result = scriptEngine.execute(compiledScript, emptyMap())

        // Verify the result
        assertEquals(30, result)
    }

    @Test
    fun testScriptEvaluation() = runBlocking {
        // Create a script engine
        val scriptEngine = JvmScriptEngine()

        // Define a simple script
        val scriptSource = """
            val numbers = listOf(1, 2, 3, 4, 5)
            val sum = numbers.sum()
            sum
        """.trimIndent()

        // Evaluate the script
        val result = scriptEngine.eval(scriptSource, "sum-script", emptyMap())

        // Verify the result
        assertEquals(15, result)
    }
}
