package ai.solace.core.scripting

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class DependencyScriptEngineTest {

    @Test
    fun testBasicMathScript() = runBlocking {
        // Create a script engine
        val scriptEngine = JvmScriptEngine()

        // Define a simple math script
        val scriptSource = """
            val a = 10
            val b = 20
            val sum = a + b
            val product = a * b
            "Sum: ${'$'}sum, Product: ${'$'}product"
        """.trimIndent()

        // Evaluate the script
        val result = scriptEngine.eval(scriptSource, "math-script")

        // Print the result for debugging
        println("[DEBUG_LOG] Math result: '$result'")

        // Verify the result
        assertTrue(result.toString().contains("Sum: 30"), "Result should contain sum")
        assertTrue(result.toString().contains("Product: 200"), "Result should contain product")
    }

    @Test
    fun testCollectionScript() = runBlocking {
        // Create a script engine
        val scriptEngine = JvmScriptEngine()

        // Define a script that uses collections
        val scriptSource = """
            val numbers = listOf(1, 2, 3, 4, 5)
            val doubled = numbers.map { it * 2 }
            val sum = doubled.sum()
            "Doubled: ${'$'}doubled, Sum: ${'$'}sum"
        """.trimIndent()

        // Evaluate the script
        val result = scriptEngine.eval(scriptSource, "collection-script")

        // Print the result for debugging
        println("[DEBUG_LOG] Collection result: '$result'")

        // Verify the result
        assertTrue(result.toString().contains("Doubled: [2, 4, 6, 8, 10]"), "Result should contain doubled list")
        assertTrue(result.toString().contains("Sum: 30"), "Result should contain sum")
    }

    @Test
    fun testDataClassScript() = runBlocking {
        // Create a script engine
        val scriptEngine = JvmScriptEngine()

        // Define a script with a data class
        val scriptSource = """
            data class Person(val name: String, val age: Int)

            val alice = Person("Alice", 30)
            "Name: ${'$'}{alice.name}, Age: ${'$'}{alice.age}"
        """.trimIndent()

        // Evaluate the script
        val result = scriptEngine.eval(scriptSource, "data-class-script")

        // Print the result for debugging
        println("[DEBUG_LOG] Data class result: '$result'")

        // Verify the result
        assertTrue(result.toString().contains("Name: Alice"), "Result should contain name")
        assertTrue(result.toString().contains("Age: 30"), "Result should contain age")
    }
}
