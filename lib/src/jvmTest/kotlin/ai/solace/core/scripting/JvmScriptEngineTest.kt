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

        // Define a simple script that uses a parameter
        val scriptSource = """
            val greeting = "Hello, " + name + "!"
            greeting
        """.trimIndent()

        // Compile the script
        val compiledScript = scriptEngine.compile(scriptSource, "greeting-script")

        // Execute the script with parameters
        val result = scriptEngine.execute(compiledScript, mapOf("name" to "World"))

        // Verify the result
        assertEquals("Hello, World!", result)
    }

    @Test
    fun testScriptEvaluation() = runBlocking {
        // Create a script engine
        val scriptEngine = JvmScriptEngine()

        // Define a simple script that uses a parameter
        val scriptSource = """
            val greeting = "Hello, " + name + "!"
            greeting
        """.trimIndent()

        // Evaluate the script with parameters
        val result = scriptEngine.eval(scriptSource, "greeting-script", mapOf("name" to "World"))

        // Verify the result
        assertEquals("Hello, World!", result)
    }
}