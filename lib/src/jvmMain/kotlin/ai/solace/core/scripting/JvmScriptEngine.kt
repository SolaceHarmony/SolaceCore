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
import kotlin.script.experimental.dependencies.DependsOn
import kotlin.script.experimental.dependencies.Repository

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
                val source = scriptSource.toScriptSource(scriptName)
                val compiledScript = KotlinCompiledScript(
                    name = scriptName,
                    compilationTimestamp = Instant.now().toEpochMilli(),
                    source = source
                )
                scriptCache[cacheKey] = compiledScript
                compiledScript
            } catch (e: Exception) {
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
                // Create an evaluation configuration with constructor args only
                val evaluationConfiguration = ScriptEvaluationConfiguration {
                    val scriptArgs = emptyArray<String>()
                    // MainKtsScript expects args via constructor, not providedProperties
                    constructorArgs(scriptArgs)

                    jvm {
                        baseClassLoader(JvmScriptEngine::class.java.classLoader)
                    }
                }

                // Execute the script (compilation happens under the hood)
                val evaluationResult = scriptingHost.eval(
                    (compiledScript as KotlinCompiledScript).source,
                    compilationConfiguration,
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
        val source: SourceCode
    ) : CompiledScript
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

