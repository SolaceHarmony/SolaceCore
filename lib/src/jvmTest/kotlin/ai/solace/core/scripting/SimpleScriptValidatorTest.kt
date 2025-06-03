package ai.solace.core.scripting

import kotlinx.coroutines.runBlocking
import kotlin.test.*

class SimpleScriptValidatorTest {

    private val validator = SimpleScriptValidator()

    @Test
    fun testValidScript() = runBlocking {
        // Define a valid script
        val scriptSource = """
            // This is a valid Kotlin script
            import ai.solace.core.actor.Actor

            val message = "Hello, World!"
            println(message)

            fun greet(name: String) {
                println("Hello, ${'$'}name!")
            }

            greet("Kotlin")
        """.trimIndent()

        // Validate the script
        val result = validator.validate(scriptSource)

        // Verify the result
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun testEmptyScript() = runBlocking {
        // Define an empty script
        val scriptSource = ""

        // Validate the script
        val result = validator.validate(scriptSource)

        // Verify the result
        assertFalse(result.isValid)
        assertEquals(1, result.errors.size)
        assertEquals("Script is empty", result.errors[0].message)
    }

    @Test
    fun testUnbalancedBraces() = runBlocking {
        // Define a script with unbalanced braces
        val scriptSource = """
            fun greet(name: String {
                println("Hello, ${'$'}name!")
            }
        """.trimIndent()

        // Validate the script
        val result = validator.validate(scriptSource)

        // Verify the result
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.errors.any { it.message.contains("Unbalanced") })
    }

    @Test
    fun testUnclosedBraces() = runBlocking {
        // Define a script with unclosed braces
        val scriptSource = """
            fun greet(name: String) {
                println("Hello, ${'$'}name!")

        """.trimIndent()

        // Validate the script
        val result = validator.validate(scriptSource)

        // Verify the result
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.errors.any { it.message.contains("Unclosed") })
    }

    @Test
    fun testMissingSemicolons() = runBlocking {
        // Define a script with multiple statements on a single line without semicolons
        val scriptSource = """
            val a = 1 val b = 2
        """.trimIndent()

        // Validate the script
        val result = validator.validate(scriptSource)

        // Verify the result
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.errors.any { it.message.contains("semicolons") })
    }

    @Test
    fun testInvalidImports() = runBlocking {
        // Define a script with invalid imports
        val scriptSource = """
            import 
            import ai.solace.core.actor.Actor;
        """.trimIndent()

        // Validate the script
        val result = validator.validate(scriptSource)

        // Verify the result
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertTrue(result.errors.any { it.message.contains("Empty import") })
        assertTrue(result.errors.any { it.message.contains("semicolons") })
    }
}
