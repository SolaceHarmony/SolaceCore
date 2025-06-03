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
    fun addActor(actor: Actor, portDefinitions: Map<String, KClass<*>> = emptyMap()): ActorBuilder {
        actors[actor.id] = actor
        
        // Store port types for validation
        for ((portName, messageType) in portDefinitions) {
            portTypes[Pair(actor.id, portName)] = messageType
        }
        
        // Add the actor to the workflow manager
        runBlocking {
            workflowManager.addActor(actor)
        }
        
        return this
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
    fun connect(
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
        runBlocking {
            workflowManager.connectActors(sourceActor, sourcePort, targetActor, targetPort)
        }
        
        return this
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
 * @return A new actor builder.
 */
fun buildActorNetwork(): ActorBuilder {
    return ActorBuilder()
}