package ai.solace.core.actor.types.scripted

import ai.solace.core.actor.Actor
import ai.solace.core.actor.ActorState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.script.Compilable
import javax.script.ScriptContext
import javax.script.ScriptEngine
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ScriptedActor(
    @OptIn(ExperimentalUuidApi::class)
    id: String = Uuid.random().toString(),
    private val scriptEngine: ScriptEngine,
    private val script: String,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : Actor(id, scope) {  // Removed unused configuration parameter

    private val scriptContext = scriptEngine.context
    private val compiledScript = try {
        if (scriptEngine is Compilable) {
            scriptEngine.compile(script)
        } else null
    } catch (e: Exception) {
        handleError(e, null)
        null
    }

    init {
        setupScriptBindings()
        loadScript()
    }

    override fun defineInterface() {
        // Define scripted interface
    }

    private fun shouldProcessMessage(message: ActorMessage): Boolean {  // Changed from protected to private
        return when (getState()) {
            ActorState.RUNNING -> true
            else -> false
        }
    }

    private fun setupScriptBindings() {
        scriptContext.apply {
            setAttribute("actor", this@ScriptedActor, ScriptContext.ENGINE_SCOPE)
            setAttribute("metrics", metrics, ScriptContext.ENGINE_SCOPE)
            setAttribute("interface", actorInterface, ScriptContext.ENGINE_SCOPE)
        }
    }

    private fun loadScript() {
        try {
            compiledScript?.eval(scriptContext) ?: scriptEngine.eval(script, scriptContext)
        } catch (e: Exception) {
            handleError(e, null)
            throw IllegalStateException("Failed to load script", e)
        }
    }

    override suspend fun processMessage(message: ActorMessage) {
        if (shouldProcessMessage(message)) {
            try {
                val scriptFunction = scriptContext.getAttribute("processMessage")
                if (scriptFunction is Function1<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    (scriptFunction as Function1<ActorMessage, Unit>).invoke(message)
                } else {
                    scriptEngine.eval("processMessage(message)", scriptContext.apply {
                        setAttribute("message", message, ScriptContext.ENGINE_SCOPE)
                    })
                }
            } catch (e: Exception) {
                handleError(e, message)
            }
        }
    }

    private fun handleError(e: Exception, message: ActorMessage?) {
        if (message != null) {
            super.handleError(e, message)
        } else {
            println("Script error: ${e.message}")
        }
    }
}