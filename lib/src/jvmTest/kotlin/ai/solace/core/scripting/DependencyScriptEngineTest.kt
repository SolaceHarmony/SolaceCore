package ai.solace.core.scripting

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DependencyScriptEngineTest {

    @Test
    fun testDependsOnAnnotation() = runBlocking {
        // Create a script engine
        val scriptEngine = JvmScriptEngine()

        // Define a script that uses @file:DependsOn to import a library
        val scriptSource = """
            @file:DependsOn("org.jetbrains.kotlinx:kotlinx-html-jvm:0.8.0")
            
            import kotlinx.html.*
            import kotlinx.html.stream.*
            
            val html = createHTML().html {
                head { 
                    title("Test Page") 
                }
                body { 
                    h1 { +"Hello from Kotlin HTML DSL" }
                    p { +"This is a test paragraph" }
                }
            }
            
            html.toString()
        """.trimIndent()

        // Evaluate the script
        val result = scriptEngine.eval(scriptSource, "html-script")

        // Print the result for debugging
        println("[DEBUG_LOG] HTML result: '$result'")

        // Verify the result contains expected HTML elements
        assertTrue(result.toString().contains("<html>"), "Result should contain <html> tag")
        assertTrue(result.toString().contains("<head>"), "Result should contain <head> tag")
        assertTrue(result.toString().contains("<title>Test Page</title>"), "Result should contain title")
        assertTrue(result.toString().contains("<h1>Hello from Kotlin HTML DSL</h1>"), "Result should contain h1")
        assertTrue(result.toString().contains("<p>This is a test paragraph</p>"), "Result should contain paragraph")
    }
}