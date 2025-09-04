package ai.solace.core.scripting

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.mainKts.MainKtsScript
import java.time.Instant
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

/**
 * JVM implementation of the ScriptEngine interface using Kotlin's scripting APIs.
 *
 * This implementation uses the kotlin.script.experimental.* APIs to compile and execute
 * Kotlin scripts. It supports proper script evaluation with access to the project's classes,
 * handles compilation and execution errors properly, and supports hot-reloading of scripts.
 */
class JvmScriptEngine : ScriptEngine {
    /**
     * A cache of compiled scripts to avoid recompilation.
     * The key is a combination of the script name and a hash of the script content.
     */
    private val scriptCache = mutableMapOf<String, KotlinCompiledScript>()

    /**
     * The scripting host used for compilation and execution.
     */
    private val scriptingHost = BasicJvmScriptingHost()

    /**
     * The compilation configuration used for compiling scripts.
     * 
     * We use MainKtsScript from kotlin-main-kts, which has built-in support for
     * @file:DependsOn and @file:Repository annotations for external dependencies.
     */
    private val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<MainKtsScript> {
        // Add any additional configuration if needed
        jvm {
            // Use the current classloader to resolve dependencies
            dependenciesFromCurrentContext(wholeClasspath = true)
        }

        // Default imports for convenience
        defaultImports("kotlin.math.*", "kotlinx.coroutines.*")

        // Enable script annotations processing
        refineConfiguration {
            beforeCompiling { context ->
                context.compilationConfiguration.asSuccess()
            }
        }
    }

    /**
     * Compiles a script from the given source code.
     *
     * @param scriptSource The source code of the script.
     * @param scriptName The name of the script.
     * @return A compiled script that can be executed.
     * @throws ScriptCompilationException if compilation fails.
     */
    override suspend fun compile(scriptSource: String, scriptName: String): CompiledScript {
        return withContext(Dispatchers.IO) {
            // Create a cache key that includes both the script name and a hash of the script content
            val cacheKey = "$scriptName:${scriptSource.hashCode()}"

            // Check if the script is already in the cache
            scriptCache[cacheKey]?.let { return@withContext it }

            try {
                // For scripts that might have parameters, we'll store the source and compile later
                // Try to compile first - if it fails due to undefined variables, we'll handle it during execution
                val source = scriptSource.toScriptSource(scriptName)
                val compilationResult = scriptingHost.compiler(source, compilationConfiguration)

                val compiledScriptResult = if (compilationResult is ResultWithDiagnostics.Success) {
                    // Successfully compiled without parameters
                    compilationResult.value
                } else {
                    // Compilation failed - this might be due to undefined variables for parameters
                    // We'll store null and handle compilation during execution
                    null
                }

                // Create a compiled script object that stores both the result and original source
                val compiledScript = KotlinCompiledScript(
                    name = scriptName,
                    compilationTimestamp = Instant.now().toEpochMilli(),
                    compiledScript = compiledScriptResult,
                    originalSource = scriptSource  // Always store the original source
                )

                // Cache the compiled script using the combined key
                scriptCache[cacheKey] = compiledScript

                compiledScript
            } catch (e: Exception) {
                if (e is ScriptCompilationException) throw e
                throw ScriptCompilationException("Script compilation failed: ${e.message}")
            }
        }
    }

    /**
     * Executes a compiled script with the given parameters.
     *
     * @param compiledScript The compiled script to execute.
     * @param parameters The parameters to pass to the script.
     * @return The result of the script execution.
     * @throws IllegalArgumentException if the compiledScript is not a KotlinCompiledScript.
     * @throws ScriptExecutionException if execution fails.
     */
    override suspend fun execute(compiledScript: CompiledScript, parameters: Map<String, Any?>): Any? {
        if (compiledScript !is KotlinCompiledScript) {
            throw IllegalArgumentException("Expected KotlinCompiledScript, got ${compiledScript::class.simpleName}")
        }

        return withContext(Dispatchers.IO) {
            try {
                // If we have parameters and original source, or if the script didn't compile initially, create a parameterized script
                if ((parameters.isNotEmpty() && compiledScript.originalSource != null) || compiledScript.compiledScript == null) {
                    if (compiledScript.originalSource == null) {
                        throw ScriptExecutionException("Cannot execute script with parameters: original source not available")
                    }
                    return@withContext executeParameterizedScript(compiledScript.originalSource, parameters, compiledScript.name)
                }

                // Otherwise, execute the compiled script normally
                val evaluationConfiguration = ScriptEvaluationConfiguration {
                    // MainKtsScript expects an "args" parameter, so we provide an empty array if not present
                    val updatedParameters = if (!parameters.containsKey("args")) {
                        parameters + ("args" to emptyArray<String>())
                    } else {
                        parameters
                    }

                    providedProperties(updatedParameters)
                    jvm {
                        baseClassLoader(JvmScriptEngine::class.java.classLoader)
                    }
                }

                // Execute the script
                val evaluationResult = scriptingHost.evaluator(
                    compiledScript.compiledScript!!,
                    evaluationConfiguration
                )

                // Check for execution errors
                if (evaluationResult is ResultWithDiagnostics.Failure) {
                    val errors = evaluationResult.reports.joinToString("\n") { it.message }
                    throw ScriptExecutionException("Script execution failed: $errors")
                }

                // Return the result of the script execution
                extractResultValue((evaluationResult as ResultWithDiagnostics.Success).value.returnValue)

            } catch (e: Exception) {
                if (e is ScriptExecutionException) throw e
                throw ScriptExecutionException("Script execution failed: ${e.message}")
            }
        }
    }

    /**
     * Execute a script with parameters by injecting them as variable declarations.
     */
    private suspend fun executeParameterizedScript(originalSource: String, parameters: Map<String, Any?>, scriptName: String): Any? {
        // Create parameter declarations
        val parameterDeclarations = parameters.entries.joinToString("\n") { (key, value) ->
            when (value) {
                is String -> "val $key = \"${value.replace("\"", "\\\"")}\""
                is Number -> "val $key = $value"
                is Boolean -> "val $key = $value"
                null -> "val $key = null"
                else -> "val $key = \"$value\""
            }
        }

        // Combine parameter declarations with the original script
        val parameterizedScript = "$parameterDeclarations\n\n$originalSource"

        // Create and execute the parameterized script
        val source = parameterizedScript.toScriptSource("${scriptName}-parameterized")
        val compilationResult = scriptingHost.compiler(source, compilationConfiguration)

        if (compilationResult is ResultWithDiagnostics.Failure) {
            val errors = compilationResult.reports.joinToString("\n") { it.message }
            throw ScriptCompilationException("Parameterized script compilation failed: $errors")
        }

        val evaluationConfiguration = ScriptEvaluationConfiguration {
            // Provide an empty args array for MainKtsScript
            providedProperties(mapOf("args" to emptyArray<String>()))
            jvm {
                baseClassLoader(JvmScriptEngine::class.java.classLoader)
            }
        }

        val evaluationResult = scriptingHost.evaluator(
            (compilationResult as ResultWithDiagnostics.Success).value,
            evaluationConfiguration
        )

        if (evaluationResult is ResultWithDiagnostics.Failure) {
            val errors = evaluationResult.reports.joinToString("\n") { it.message }
            throw ScriptExecutionException("Parameterized script execution failed: $errors")
        }

        return extractResultValue((evaluationResult as ResultWithDiagnostics.Success).value.returnValue)
    }

    /**
     * Extract the actual result value from a ResultValue.
     */
    private fun extractResultValue(resultValue: ResultValue): Any? {
        return when (resultValue) {
            is ResultValue.Value -> {
                // Extract the actual value from the ResultValue.Value object
                val valueString = resultValue.toString()
                if (valueString.contains("=")) {
                    // Extract the value after the equals sign
                    val extractedValue = valueString.substringAfter("=").trim()

                    // Try to convert numeric strings to their appropriate types
                    when {
                        // Check if it's an integer
                        extractedValue.toIntOrNull() != null -> extractedValue.toInt()
                        // Check if it's a double
                        extractedValue.toDoubleOrNull() != null -> extractedValue.toDouble()
                        // Otherwise, return as string
                        else -> extractedValue
                    }
                } else {
                    // If there's no equals sign, return the value as is
                    resultValue.value
                }
            }
            else -> resultValue
        }
    }

    /**
     * Compiles and executes a script in one step.
     *
     * @param scriptSource The source code of the script.
     * @param scriptName The name of the script.
     * @param parameters The parameters to pass to the script.
     * @return The result of the script execution.
     * @throws ScriptCompilationException if compilation fails.
     * @throws ScriptExecutionException if execution fails.
     */
    override suspend fun eval(scriptSource: String, scriptName: String, parameters: Map<String, Any?>): Any? {
        return withContext(Dispatchers.IO) {
            try {
                // Compile the script
                val compiledScript = compile(scriptSource, scriptName)

                // Execute the compiled script
                execute(compiledScript, parameters)
            } catch (e: Exception) {
                if (e is ScriptCompilationException || e is ScriptExecutionException) throw e

                // Try to determine if it's a compilation or execution error
                if (e.message?.contains("compile") == true || e.message?.contains("syntax") == true) {
                    throw ScriptCompilationException("Script compilation failed: ${e.message}")
                } else {
                    throw ScriptExecutionException("Script execution failed: ${e.message}")
                }
            }
        }
    }

    /**
     * A Kotlin implementation of the CompiledScript interface.
     */
    private class KotlinCompiledScript(
        override val name: String,
        override val compilationTimestamp: Long,
        val compiledScript: kotlin.script.experimental.api.CompiledScript?,  // Can be null if deferred compilation
        val originalSource: String? = null  // Store original source for parameter injection
    ) : ai.solace.core.scripting.CompiledScript
}

// We're using MainKtsScript from kotlin-main-kts, which already has built-in support
// for @file:DependsOn and @file:Repository annotations for external dependencies.

/**
 * Exception thrown when script compilation fails.
 */
class ScriptCompilationException(message: String) : Exception(message)

/**
 * Exception thrown when script execution fails.
 */
class ScriptExecutionException(message: String) : Exception(message)
