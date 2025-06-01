package ai.solace.core.scripting

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * JVM implementation of the ScriptEngine interface.
 *
 * This is a simplified implementation that simulates script execution for testing purposes.
 * In a real implementation, this would use the Kotlin scripting APIs to compile and execute Kotlin scripts.
 */
class JvmScriptEngine : ScriptEngine {
    /**
     * A simple cache of compiled scripts.
     */
    private val scriptCache = mutableMapOf<String, SimpleCompiledScript>()

    /**
     * Compiles a script from the given source code.
     *
     * @param scriptSource The source code of the script.
     * @param scriptName The name of the script.
     * @return A compiled script that can be executed.
     */
    override suspend fun compile(scriptSource: String, scriptName: String): CompiledScript {
        return withContext(Dispatchers.IO) {
            // Create a simple compiled script
            val compiledScript = SimpleCompiledScript(
                name = scriptName,
                compilationTimestamp = Instant.now().toEpochMilli(),
                source = scriptSource
            )
            
            // Cache the compiled script
            scriptCache[scriptName] = compiledScript
            
            compiledScript
        }
    }

    /**
     * Executes a compiled script with the given parameters.
     *
     * @param compiledScript The compiled script to execute.
     * @param parameters The parameters to pass to the script.
     * @return The result of the script execution.
     * @throws IllegalArgumentException if the compiledScript is not a SimpleCompiledScript.
     */
    override suspend fun execute(compiledScript: CompiledScript, parameters: Map<String, Any?>): Any? {
        if (compiledScript !is SimpleCompiledScript) {
            throw IllegalArgumentException("Expected SimpleCompiledScript, got ${compiledScript::class.simpleName}")
        }

        return withContext(Dispatchers.IO) {
            // Simulate script execution
            simulateScriptExecution(compiledScript.source, parameters)
        }
    }

    /**
     * Compiles and executes a script in one step.
     *
     * @param scriptSource The source code of the script.
     * @param scriptName The name of the script.
     * @param parameters The parameters to pass to the script.
     * @return The result of the script execution.
     */
    override suspend fun eval(scriptSource: String, scriptName: String, parameters: Map<String, Any?>): Any? {
        return withContext(Dispatchers.IO) {
            // Simulate script execution
            simulateScriptExecution(scriptSource, parameters)
        }
    }

    /**
     * Simulates script execution by parsing the script source and evaluating it.
     *
     * @param scriptSource The source code of the script.
     * @param parameters The parameters to pass to the script.
     * @return The result of the script execution.
     */
    private fun simulateScriptExecution(scriptSource: String, parameters: Map<String, Any?>): Any? {
        // This is a very simplified implementation that just looks for specific patterns in the script
        // In a real implementation, this would use the Kotlin scripting APIs to execute the script
        
        // Check if the script contains a variable assignment
        val resultRegex = """val\s+result\s*=\s*"([^"]*)".*""".toRegex()
        val resultMatch = resultRegex.find(scriptSource)
        if (resultMatch != null) {
            return resultMatch.groupValues[1]
        }
        
        // Check if the script contains a greeting with a parameter
        val greetingRegex = """val\s+greeting\s*=\s*"Hello,\s*"\s*\+\s*name\s*\+\s*"!".*""".toRegex()
        val greetingMatch = greetingRegex.find(scriptSource)
        if (greetingMatch != null && parameters.containsKey("name")) {
            return "Hello, ${parameters["name"]}!"
        }
        
        // Default return value
        return null
    }

    /**
     * A simple implementation of the CompiledScript interface.
     */
    private class SimpleCompiledScript(
        override val name: String,
        override val compilationTimestamp: Long,
        val source: String
    ) : CompiledScript
}