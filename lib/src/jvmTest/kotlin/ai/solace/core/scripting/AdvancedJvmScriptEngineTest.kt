package ai.solace.core.scripting

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdvancedJvmScriptEngineTest {

    @Test
    fun testDataClassScript() = runBlocking {
        // Create a script engine
        val scriptEngine = JvmScriptEngine()

        // Define a script with a data class
        val scriptSource = """
            data class Person(val name: String, val age: Int)

            val person = Person("Alice", 30)
            "Name: " + person.name + ", Age: " + person.age
        """.trimIndent()

        // Evaluate the script
        val result = scriptEngine.eval(scriptSource, "data-class-script")

        // Print the result for debugging
        println("[DEBUG_LOG] Data class result: '$result', type: ${result?.javaClass}")

        // Verify the result
        assertEquals("Name: Alice, Age: 30", result)
    }

    @Test
    fun testExtensionFunctionScript() = runBlocking {
        // Create a script engine
        val scriptEngine = JvmScriptEngine()

        // Define a script with an extension function
        val scriptSource = """
            fun String.addExclamation(): String = this + "!"

            val message = "Hello"
            message.addExclamation()
        """.trimIndent()

        // Evaluate the script
        val result = scriptEngine.eval(scriptSource, "extension-function-script")

        // Verify the result
        assertEquals("Hello!", result)
    }

    @Test
    fun testWhenExpressionScript() = runBlocking {
        // Create a script engine
        val scriptEngine = JvmScriptEngine()

        // Define a script with a when expression
        val scriptSource = """
            val number = 42

            val result = when {
                number < 0 -> "Negative"
                number == 0 -> "Zero"
                number < 10 -> "Small"
                number < 100 -> "Medium"
                else -> "Large"
            }

            result
        """.trimIndent()

        // Evaluate the script
        val result = scriptEngine.eval(scriptSource, "when-expression-script")

        // Verify the result
        assertEquals("Medium", result)
    }

    @Test
    fun testLambdaScript() = runBlocking {
        // Create a script engine
        val scriptEngine = JvmScriptEngine()

        // Define a script with lambdas and higher-order functions
        val scriptSource = """
            val numbers = listOf(1, 2, 3, 4, 5)

            val doubled = numbers.map { it * 2 }
            val sum = doubled.sum()

            sum
        """.trimIndent()

        // Evaluate the script
        val result = scriptEngine.eval(scriptSource, "lambda-script")

        // Print the result for debugging
        println("[DEBUG_LOG] Lambda result: '$result', type: ${result?.javaClass}")

        // Verify the result
        assertEquals(30, result)
    }
}
