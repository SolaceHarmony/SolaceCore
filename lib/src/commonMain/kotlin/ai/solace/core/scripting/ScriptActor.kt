@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package ai.solace.core.scripting

import ai.solace.core.actor.Actor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.reflect.KClass
import kotlin.uuid.Uuid

/**
 * An actor that uses a script for its behavior.
 *
 * This actor delegates its processing logic to a Kotlin script, which can be
 * hot-reloaded at runtime without stopping the actor.
 *
 * @param id Unique identifier of the actor, defaults to a random UUID.
 * @param name Name of the actor, defaults to "ScriptActor".
 * @param scriptEngine The script engine to use for compiling and executing scripts.
 * @param scriptSource The initial source code of the script.
 * @param scriptName The name of the script.
 * @param scope Coroutine scope used by the actor, defaults to a new scope with default dispatcher and supervisor job.
 */
class ScriptActor(
    id: String = Uuid.random().toString(),
    name: String = "ScriptActor",
    private val scriptEngine: ScriptEngine,
    private var scriptSource: String,
    private val scriptName: String,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : Actor(id, name, scope) {

    /**
     * The compiled script.
     */
    private var compiledScript: CompiledScript? = null

    /**
     * Initializes the actor by compiling the script and creating the necessary ports.
     *
     * @param inputPortName The name of the input port.
     * @param inputMessageClass The class of the input messages.
     * @param outputPortName The name of the output port.
     * @param outputMessageClass The class of the output messages.
     */
    suspend fun <I : Any, O : Any> initialize(
        inputPortName: String,
        inputMessageClass: KClass<I>,
        outputPortName: String,
        outputMessageClass: KClass<O>
    ) {
        // Compile the script
        compiledScript = scriptEngine.compile(scriptSource, scriptName)

        // Create input port
        createPort(
            name = inputPortName,
            messageClass = inputMessageClass,
            handler = { message -> processMessage(message) },
            bufferSize = 10
        )

        // Create output port
        createPort(
            name = outputPortName,
            messageClass = outputMessageClass,
            handler = { /* This port is only used for output, so no handler is needed */ },
            bufferSize = 10
        )
    }

    /**
     * Processes a message by executing the script.
     *
     * @param message The message to process.
     */
    private suspend fun <T : Any> processMessage(message: T) {
        val script = compiledScript ?: throw IllegalStateException("Script not initialized")
        
        // Create parameters for the script
        val parameters = mapOf(
            "message" to message,
            "actor" to this
        )
        
        // Execute the script
        scriptEngine.execute(script, parameters)
    }

    /**
     * Reloads the script with a new source.
     *
     * @param newScriptSource The new source code for the script.
     */
    suspend fun reloadScript(newScriptSource: String) {
        // Store the new script source
        scriptSource = newScriptSource
        
        // Compile the new script
        compiledScript = scriptEngine.compile(newScriptSource, scriptName)
    }

    /**
     * Gets the current script source.
     *
     * @return The current script source.
     */
    fun getScriptSource(): String {
        return scriptSource
    }

    /**
     * Gets the compiled script.
     *
     * @return The compiled script, or null if the script hasn't been compiled yet.
     */
    fun getCompiledScript(): CompiledScript? {
        return compiledScript
    }

    /**
     * Starts the actor, initializing it if necessary.
     */
    override suspend fun start() {
        // Check if the script has been compiled
        if (compiledScript == null) {
            compiledScript = scriptEngine.compile(scriptSource, scriptName)
        }
        
        // Call the parent start method
        super.start()
    }
}
