package ai.solace.core.scripting

/**
 * Interface for the script engine that compiles and executes Kotlin scripts.
 */
interface ScriptEngine {
    /**
     * Compiles a script from the given source code.
     *
     * @param scriptSource The source code of the script.
     * @param scriptName The name of the script.
     * @return A compiled script that can be executed.
     */
    suspend fun compile(scriptSource: String, scriptName: String): CompiledScript

    /**
     * Executes a compiled script with the given parameters.
     *
     * @param compiledScript The compiled script to execute.
     * @param parameters The parameters to pass to the script.
     * @return The result of the script execution.
     */
    suspend fun execute(compiledScript: CompiledScript, parameters: Map<String, Any?>): Any?

    /**
     * Compiles and executes a script in one step.
     *
     * @param scriptSource The source code of the script.
     * @param scriptName The name of the script.
     * @param parameters The parameters to pass to the script.
     * @return The result of the script execution.
     */
    suspend fun eval(scriptSource: String, scriptName: String, parameters: Map<String, Any?> = emptyMap()): Any?
}