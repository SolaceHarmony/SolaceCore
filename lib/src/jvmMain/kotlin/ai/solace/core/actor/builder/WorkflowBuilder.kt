package ai.solace.core.actor.builder

import ai.solace.core.actor.Actor
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KClass

/**
 * Builder class for constructing a workflow consisting of various actors and their connections.
 *
 * @param scope The CoroutineScope in which the actors will operate.
 */
class WorkflowBuilder(private val scope: CoroutineScope) {
    /**
     * A collection of actors that constitutes the components of the workflow.
     * Each actor in this list takes part in the workflow's processing logic.
     */
    private val actors = mutableListOf<Actor>()
    /**
     * A collection of connections between actors within the workflow.
     *
     * Each connection specifies how the output port of one actor is connected to the input port of another actor.
     * The connections are stored as instances of the `Connection` data class.
     */
    private val connections = mutableListOf<Connection<*>>()

    /**
     * Represents a connection between two actors within a workflow.
     *
     * @param T The type of data being transferred through this connection.
     * @property fromActor The actor from which the data is sent.
     * @property fromPort The specific port on the fromActor from which the data is sent.
     * @property toActor The actor to which the data is delivered.
     * @property toPort The specific port on the toActor to which the data is delivered.
     * @property type The class type of the data being transferred.
     */
    data class Connection<T : Any>(
        val fromActor: Actor,
        val fromPort: String,
        val toActor: Actor,
        val toPort: String,
        val type: KClass<T>
    )

    /**
     * Adds the specified actor to the workflow.
     *
     * @param actor The actor to be added to the workflow.
     * @return The current instance of `WorkflowBuilder` to allow for method chaining.
     */
    fun addActor(actor: Actor): WorkflowBuilder {
        actors.add(actor)
        return this
    }

    /**
     * Connects two actors' ports with a specified type.
     *
     * @param fromActor The actor from which the connection originates.
     * @param fromPort The port on the `fromActor` from which data will be sent.
     * @param toActor The actor to which the connection is made.
     * @param toPort The port on the `toActor` to which data will be received.
     * @param type The type of data that will be transmitted through this connection.
     * @return An instance of `WorkflowBuilder` to allow for method chaining.
     */
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

}