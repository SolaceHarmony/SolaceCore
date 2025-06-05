package ai.solace.core.scripting

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CompiledScriptTest {

    @Test
    fun testCompiledScriptProperties() = runBlocking {
        // Create a script engine
        val scriptEngine = JvmScriptEngine()

        // Define a simple script
        val scriptSource = """
            val result = "Hello, World!"
            result
        """.trimIndent()

        // Compile the script
        val compiledScript = scriptEngine.compile(scriptSource, "test-script")

        // Verify the compiled script properties
        assertNotNull(compiledScript)
        assertEquals("test-script", compiledScript.name)
        assertTrue(compiledScript.compilationTimestamp > 0, "Compilation timestamp should be positive")
    }

    @Test
    fun testCompiledScriptExecution() = runBlocking {
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
    fun testCompiledScriptWithParameters() = runBlocking {
        // Create a script engine
        val scriptEngine = JvmScriptEngine()

        // Define a script that uses parameters
        val scriptSource = """
            val x = x as Int
            val y = y as Int
            x * y
        """.trimIndent()

        // Compile the script
        val compiledScript = scriptEngine.compile(scriptSource, "param-script")

        // Execute the script with parameters
        val parameters = mapOf("x" to 5, "y" to 7)
        val result = scriptEngine.execute(compiledScript, parameters)

        // Verify the result
        assertEquals(35, result)
    }

    @Test
    fun testCompiledScriptReuse() = runBlocking {
        // Create a script engine
        val scriptEngine = JvmScriptEngine()

        // Define a script that uses parameters
        val scriptSource = """
            val factor = factor as Int
            val numbers = listOf(1, 2, 3, 4, 5)
            numbers.map { it * factor }.sum()
        """.trimIndent()

        // Compile the script once
        val compiledScript = scriptEngine.compile(scriptSource, "reuse-script")

        // Execute the script multiple times with different parameters
        val result1 = scriptEngine.execute(compiledScript, mapOf("factor" to 2))
        val result2 = scriptEngine.execute(compiledScript, mapOf("factor" to 3))
        val result3 = scriptEngine.execute(compiledScript, mapOf("factor" to 4))

        // Verify the results
        assertEquals(30, result1) // (1+2+3+4+5)*2 = 30
        assertEquals(45, result2) // (1+2+3+4+5)*3 = 45
        assertEquals(60, result3) // (1+2+3+4+5)*4 = 60
    }
}
