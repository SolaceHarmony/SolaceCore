package ai.solace.core.actor.builder

import ai.solace.core.actor.Actor
import ai.solace.core.workflow.WorkflowManager
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass

/**
 * A builder for creating and connecting actors in a type-safe manner.
 *
 * This builder provides a fluent API for:
 * - Creating actors
 * - Adding actors to a workflow
 * - Connecting actors' ports
 * - Validating connections between actors
 */
class ActorBuilder {
    /**
     * The workflow manager that will manage the actors.
     */
    private val workflowManager = WorkflowManager()

    /**
     * Map of actors added to the builder, keyed by their IDs.
     */
    private val actors = mutableMapOf<String, Actor>()

    /**
     * Map of port types for each actor and port, keyed by actor ID and port name.
     */
    private val portTypes = mutableMapOf<Pair<String, String>, KClass<*>>()

    /**
     * Adds an actor to the builder.
     *
     * @param actor The actor to add.
     * @param portDefinitions The port definitions for the actor, mapping port names to message types.
     * @return This builder for method chaining.
     */
    suspend fun addActor(actor: Actor, portDefinitions: Map<String, KClass<*>> = emptyMap()): ActorBuilder {
        actors[actor.id] = actor

        // Store port types for validation
        for ((portName, messageType) in portDefinitions) {
            portTypes[Pair(actor.id, portName)] = messageType
        }

        // Add the actor to the workflow manager
        workflowManager.addActor(actor)

        return this
    }

    /**
     * Adds an actor to the builder (blocking version).
     *
     * This method is provided for backward compatibility. It's recommended to use the suspend version
     * of this method in a coroutine context to avoid blocking threads.
     *
     * @param actor The actor to add.
     * @param portDefinitions The port definitions for the actor, mapping port names to message types.
     * @return This builder for method chaining.
     */
    @Deprecated("Use suspend version of addActor to avoid blocking threads", ReplaceWith("runBlocking { addActor(actor, portDefinitions) }"))
    fun addActorBlocking(actor: Actor, portDefinitions: Map<String, KClass<*>> = emptyMap()): ActorBuilder {
        return runBlocking {
            addActor(actor, portDefinitions)
        }
    }

    /**
     * Connects two actors' ports.
     *
     * @param sourceActor The source actor.
     * @param sourcePort The name of the source port.
     * @param targetActor The target actor.
     * @param targetPort The name of the target port.
     * @return This builder for method chaining.
     * @throws IllegalArgumentException if the port types are incompatible.
     */
    suspend fun connect(
        sourceActor: Actor,
        sourcePort: String,
        targetActor: Actor,
        targetPort: String
    ): ActorBuilder {
        // Validate port types if available
        val sourceType = portTypes[Pair(sourceActor.id, sourcePort)]
        val targetType = portTypes[Pair(targetActor.id, targetPort)]

        if (sourceType != null && targetType != null && sourceType != targetType) {
            throw IllegalArgumentException(
                "Incompatible port types: ${sourceType.simpleName} (${sourceActor.name}.$sourcePort) " +
                "and ${targetType.simpleName} (${targetActor.name}.$targetPort)"
            )
        }

        // Connect the actors in the workflow manager
        workflowManager.connectActors(sourceActor, sourcePort, targetActor, targetPort)

        return this
    }

    /**
     * Connects two actors' ports (blocking version).
     *
     * This method is provided for backward compatibility. It's recommended to use the suspend version
     * of this method in a coroutine context to avoid blocking threads.
     *
     * @param sourceActor The source actor.
     * @param sourcePort The name of the source port.
     * @param targetActor The target actor.
     * @param targetPort The name of the target port.
     * @return This builder for method chaining.
     * @throws IllegalArgumentException if the port types are incompatible.
     */
    @Deprecated("Use suspend version of connect to avoid blocking threads", ReplaceWith("runBlocking { connect(sourceActor, sourcePort, targetActor, targetPort) }"))
    fun connectBlocking(
        sourceActor: Actor,
        sourcePort: String,
        targetActor: Actor,
        targetPort: String
    ): ActorBuilder {
        return runBlocking {
            connect(sourceActor, sourcePort, targetActor, targetPort)
        }
    }

    /**
     * Builds and returns the workflow manager with all added actors and connections.
     *
     * @return The workflow manager.
     */
    fun build(): WorkflowManager {
        return workflowManager
    }
}

/**
 * Creates a new actor builder.
 *
 * This method is provided for backward compatibility. It's recommended to use the suspend version
 * of this method in a coroutine context to avoid blocking threads.
 *
 * @return A new actor builder.
 */
@Deprecated("Use buildActorNetworkAsync in a coroutine context to avoid blocking threads", ReplaceWith("runBlocking { buildActorNetworkAsync() }"))
fun buildActorNetwork(): ActorBuilder {
    return ActorBuilder()
}

/**
 * Creates a new actor builder for use in a coroutine context.
 *
 * This is the preferred way to create an ActorBuilder as it allows for non-blocking
 * operations when adding actors and connecting ports.
 *
 * @return A new actor builder.
 */
suspend fun buildActorNetworkAsync(): ActorBuilder {
    return ActorBuilder()
}
