package ai.solace.core.workflow

import ai.solace.core.actor.Actor
import ai.solace.core.actor.interfaces.Port
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.reflect.KClass

class WorkflowManager(private val scope: CoroutineScope) {
    private val actors = mutableMapOf<String, Actor>()
    private val _state = MutableStateFlow<WorkflowState>(WorkflowState.Idle)
    val state: StateFlow<WorkflowState> = _state

    sealed class WorkflowState {
        object Idle : WorkflowState()
        object Running : WorkflowState()
        data class Error(val message: String) : WorkflowState()
    }

    fun addActor(actor: Actor) {
        actors[actor.id] = actor
    }

    fun <T : Any> connect(
        fromActor: Actor,
        fromPort: String,
        toActor: Actor,
        toPort: String,
        type: KClass<T>
    ) {
        val outputPort = fromActor.getInterface().getOutput(fromPort) as? Port.Output<T>
            ?: throw IllegalStateException("Output port $fromPort not found or wrong type")
        
        val inputPort = toActor.getInterface().getInput(toPort) as? Port.Input<T>
            ?: throw IllegalStateException("Input port $toPort not found or wrong type")

        fromActor.getInterface().connect(outputPort, inputPort)
    }

    fun start() {
        scope.launch {
            try {
                _state.value = WorkflowState.Running
                actors.values.forEach { it.start() }
            } catch (e: Exception) {
                _state.value = WorkflowState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun stop() {
        scope.launch {
            try {
                actors.values.forEach { it.stop() }
                _state.value = WorkflowState.Idle
            } catch (e: Exception) {
                _state.value = WorkflowState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun getActor(id: String): Actor? = actors[id]

    fun getAllActors(): List<Actor> = actors.values.toList()

    companion object {
        fun builder(scope: CoroutineScope) = WorkflowBuilder(scope)
    }
}

class WorkflowBuilder(private val scope: CoroutineScope) {
    private val actors = mutableListOf<Actor>()
    private val connections = mutableListOf<Connection<*>>()

    data class Connection<T : Any>(
        val fromActor: Actor,
        val fromPort: String,
        val toActor: Actor,
        val toPort: String,
        val type: KClass<T>
    )

    fun addActor(actor: Actor): WorkflowBuilder {
        actors.add(actor)
        return this
    }

    fun <T : Any> connect(
        fromActor: Actor,
        fromPort: String,
        toActor: Actor,
        toPort: String,
        type: KClass<T>
    ): WorkflowBuilder {
        connections.add(Connection(fromActor, fromPort, toActor, toPort, type))
        return this
    }

    fun build(): WorkflowManager {
        val workflow = WorkflowManager(scope)
        
        // Add all actors
        actors.forEach { workflow.addActor(it) }

        // Create all connections
        connections.forEach { conn ->
            workflow.connect(
                conn.fromActor,
                conn.fromPort,
                conn.toActor,
                conn.toPort,
                conn.type
            )
        }

        return workflow
    }
}