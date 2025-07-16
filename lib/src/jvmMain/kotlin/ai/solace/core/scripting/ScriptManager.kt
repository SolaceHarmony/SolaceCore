package ai.solace.core.scripting

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages scripts by integrating the script engine, storage, version manager, and validator.
 *
 * The ScriptManager is the main entry point for the scripting system in the SolaceCore framework.
 * It provides a unified interface for all script-related operations, abstracting away the details
 * of how scripts are compiled, executed, stored, versioned, and validated.
 *
 * Key features:
 * - **Compilation and Execution**: Compiles scripts using the script engine and executes them with parameters.
 * - **Validation**: Ensures scripts are syntactically correct and follow best practices.
 * - **Storage and Retrieval**: Persists scripts and their metadata for later use.
 * - **Versioning and Rollback**: Maintains a history of script versions and allows rolling back to previous versions.
 * - **Hot-Reloading**: Supports reloading scripts at runtime without restarting the application.
 *
 * Usage example:
 * ```kotlin
 * val scriptManager = ScriptManager(
 *     scriptEngine = JvmScriptEngine(),
 *     scriptStorage = FileScriptStorage("scripts"),
 *     scriptVersionManager = FileScriptVersionManager("scripts", scriptStorage),
 *     scriptValidator = SimpleScriptValidator()
 * )
 *
 * // Compile and save a script
 * val script = scriptManager.compileAndSave("greeting", """
 *     val greeting = "Hello, " + name + "!"
 *     greeting
 * """)
 *
 * // Execute the script with parameters
 * val result = scriptManager.execute("greeting", mapOf("name" to "World"))
 * println(result) // Outputs: Hello, World!
 * ```
 *
 * @param scriptEngine The script engine to use for compilation and execution.
 * @param scriptStorage The script storage to use for persistence.
 * @param scriptVersionManager The script version manager to use for versioning.
 * @param scriptValidator The script validator to use for validation.
 */
class ScriptManager(
    private val scriptEngine: ScriptEngine,
    private val scriptStorage: ScriptStorage,
    private val scriptVersionManager: ScriptVersionManager,
    private val scriptValidator: ScriptValidator
) {
    /**
     * A cache of compiled scripts.
     */
    private val compiledScriptCache = mutableMapOf<String, CompiledScript>()

    /**
     * Compiles and saves a script.
     *
     * @param scriptName The name of the script.
     * @param scriptSource The source code of the script.
     * @param metadata Additional metadata for the script.
     * @return The compiled script.
     * @throws ScriptValidationException if the script fails validation.
     * @throws ScriptCompilationException if the script fails compilation.
     */
    suspend fun compileAndSave(
        scriptName: String,
        scriptSource: String,
        metadata: Map<String, Any> = emptyMap()
    ): CompiledScript {
        return withContext(Dispatchers.IO) {
            // Validate the script
            val validationResult = scriptValidator.validate(scriptSource)
            if (!validationResult.isValid) {
                val errors = validationResult.errors.joinToString("\n") { 
                    "${it.message} at line ${it.line}, column ${it.column}" 
                }
                throw ScriptValidationException("Script validation failed: $errors")
            }

            // Compile the script
            val compiledScript = scriptEngine.compile(scriptSource, scriptName)

            // Save the script
            val updatedMetadata = metadata + mapOf(
                "compilationTimestamp" to compiledScript.compilationTimestamp
            )
            scriptStorage.saveScript(scriptName, scriptSource, updatedMetadata)

            // Add a new version
            scriptVersionManager.addVersion(scriptName, scriptSource)

            // Cache the compiled script
            compiledScriptCache[scriptName] = compiledScript

            compiledScript
        }
    }

    /**
     * Loads and compiles a script.
     *
     * @param scriptName The name of the script.
     * @return The compiled script, or null if the script doesn't exist.
     * @throws ScriptCompilationException if the script fails compilation.
     */
    suspend fun loadAndCompile(scriptName: String): CompiledScript? {
        return withContext(Dispatchers.IO) {
            // Check if the script is already cached
            compiledScriptCache[scriptName]?.let { return@withContext it }

            // Load the script
            val scriptData = scriptStorage.loadScript(scriptName) ?: return@withContext null
            val (scriptSource, metadata) = scriptData

            // Compile the script
            val compiledScript = scriptEngine.compile(scriptSource, scriptName)

            // Cache the compiled script
            compiledScriptCache[scriptName] = compiledScript

            compiledScript
        }
    }

    /**
     * Executes a script.
     *
     * @param scriptName The name of the script.
     * @param parameters The parameters to pass to the script.
     * @return The result of the script execution, or null if the script doesn't exist.
     * @throws ScriptCompilationException if the script fails compilation.
     * @throws ScriptExecutionException if the script fails execution.
     */
    suspend fun execute(scriptName: String, parameters: Map<String, Any?> = emptyMap()): Any? {
        return withContext(Dispatchers.IO) {
            // Load and compile the script
            val compiledScript = loadAndCompile(scriptName) ?: return@withContext null

            // Execute the script
            scriptEngine.execute(compiledScript, parameters)
        }
    }

    /**
     * Reloads a script from storage.
     *
     * @param scriptName The name of the script.
     * @return The compiled script, or null if the script doesn't exist.
     * @throws ScriptCompilationException if the script fails compilation.
     */
    suspend fun reloadScript(scriptName: String): CompiledScript? {
        return withContext(Dispatchers.IO) {
            // Remove the script from the cache
            compiledScriptCache.remove(scriptName)

            // Load the script from storage
            val scriptData = scriptStorage.loadScript(scriptName) ?: return@withContext null
            val (scriptSource, metadata) = scriptData

            // Compile the script
            val compiledScript = scriptEngine.compile(scriptSource, scriptName)

            // Update the cache
            compiledScriptCache[scriptName] = compiledScript

            compiledScript
        }
    }

    /**
     * Rolls back to a previous version of a script.
     *
     * @param scriptName The name of the script.
     * @param version The version to roll back to.
     * @return The compiled script, or null if the rollback failed.
     * @throws ScriptCompilationException if the script fails compilation.
     */
    suspend fun rollback(scriptName: String, version: Int): CompiledScript? {
        return withContext(Dispatchers.IO) {
            // Roll back to the previous version
            val success = scriptVersionManager.rollback(scriptName, version)
            if (!success) {
                return@withContext null
            }

            // Get the rolled back script source
            val scriptSource = scriptVersionManager.getVersion(scriptName, version)
                ?: return@withContext null

            // Compile the script
            val compiledScript = scriptEngine.compile(scriptSource, scriptName)

            // Update the cache
            compiledScriptCache[scriptName] = compiledScript

            compiledScript
        }
    }

    /**
     * Lists all available scripts.
     *
     * @return A list of script names.
     */
    suspend fun listScripts(): List<String> {
        return scriptStorage.listScripts()
    }

    /**
     * Deletes a script.
     *
     * @param scriptName The name of the script.
     * @return True if the script was deleted, false otherwise.
     */
    suspend fun deleteScript(scriptName: String): Boolean {
        return withContext(Dispatchers.IO) {
            // Remove the script from the cache
            compiledScriptCache.remove(scriptName)

            // Delete the script
            scriptStorage.deleteScript(scriptName)
        }
    }
}

/**
 * Exception thrown when script validation fails.
 */
class ScriptValidationException(message: String) : Exception(message)
