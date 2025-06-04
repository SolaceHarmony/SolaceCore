package ai.solace.core.actor.supervisor

import ai.solace.core.actor.Actor
import ai.solace.core.actor.ActorState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass

/**
 * SupervisorActor is responsible for managing the lifecycle of other actors,
 * including registration, unregistration, and hot-swapping.
 *
 * It maintains a registry of available actors and their interfaces, allowing
 * for dynamic actor management at runtime without requiring system restarts.
 */
class SupervisorActor : Actor() {
    /**
     * Mutex for synchronizing access to the actor registry.
     */
    private val registryMutex = Mutex()

    /**
     * Registry of actors managed by this supervisor.
     */
    private val actorRegistry = mutableMapOf<String, Actor>()

    /**
     * Registry of actor types, mapping actor IDs to their class types.
     */
    private val actorTypeRegistry = mutableMapOf<String, KClass<out Actor>>()

    /**
     * Registers an actor with the supervisor.
     *
     * @param actor The actor to register.
     * @return True if the actor was successfully registered, false if an actor with the same ID already exists.
     * @throws IllegalStateException if the supervisor is not in the Running state.
     */
    suspend fun registerActor(actor: Actor): Boolean = registryMutex.withLock {
        if (state != ActorState.Running) {
            throw IllegalStateException("Cannot register actor while supervisor is in state: $state")
        }

        if (actorRegistry.containsKey(actor.id)) {
            return false
        }

        actorRegistry[actor.id] = actor
        actorTypeRegistry[actor.id] = actor::class

        return true
    }

    /**
     * Unregisters an actor from the supervisor.
     *
     * @param actorId The ID of the actor to unregister.
     * @return True if the actor was successfully unregistered, false if no actor with the given ID exists.
     * @throws IllegalStateException if the supervisor is not in the Running state.
     */
    suspend fun unregisterActor(actorId: String): Boolean = registryMutex.withLock {
        if (state != ActorState.Running) {
            throw IllegalStateException("Cannot unregister actor while supervisor is in state: $state")
        }

        val actor = actorRegistry.remove(actorId) ?: return false
        actorTypeRegistry.remove(actorId)

        return true
    }

    /**
     * Hot-swaps an actor with a new instance of the same type.
     *
     * @param oldActorId The ID of the actor to replace.
     * @param newActor The new actor instance to replace the old one with.
     * @return True if the actor was successfully hot-swapped, false if no actor with the given ID exists
     *         or if the new actor is not of the same type as the old one.
     * @throws IllegalStateException if the supervisor is not in the Running state.
     */
    suspend fun hotSwapActor(oldActorId: String, newActor: Actor): Boolean = registryMutex.withLock {
        if (state != ActorState.Running) {
            throw IllegalStateException("Cannot hot-swap actor while supervisor is in state: $state")
        }

        val oldActor = actorRegistry[oldActorId] ?: return false
        val oldActorType = actorTypeRegistry[oldActorId] ?: return false

        // Check if the new actor is of the same type as the old one
        if (newActor::class != oldActorType) {
            return false
        }

        // Check if the old actor is running before stopping it
        val wasRunning = oldActor.state == ActorState.Running

        // Stop the old actor
        oldActor.stop()

        // Replace the old actor with the new one
        actorRegistry[oldActorId] = newActor

        // Start the new actor if the old one was running
        if (wasRunning) {
            newActor.start()
        }

        return true
    }

    /**
     * Gets an actor by its ID.
     *
     * @param actorId The ID of the actor to get.
     * @return The actor with the specified ID, or null if no such actor exists.
     */
    suspend fun getActor(actorId: String): Actor? = registryMutex.withLock {
        return actorRegistry[actorId]
    }

    /**
     * Gets all actors managed by this supervisor.
     *
     * @return A list of all actors managed by this supervisor.
     */
    suspend fun getAllActors(): List<Actor> = registryMutex.withLock {
        return actorRegistry.values.toList()
    }

    /**
     * Gets all actors of a specific type managed by this supervisor.
     *
     * @param actorType The type of actors to get.
     * @return A list of all actors of the specified type managed by this supervisor.
     */
    suspend fun getActorsByType(actorType: KClass<out Actor>): List<Actor> = registryMutex.withLock {
        return actorRegistry.values.filter { it::class == actorType }
    }

    /**
     * Starts all actors managed by this supervisor.
     *
     * @throws IllegalStateException if the supervisor is not in the Running state.
     */
    suspend fun startAllActors() = registryMutex.withLock {
        if (state != ActorState.Running) {
            throw IllegalStateException("Cannot start actors while supervisor is in state: $state")
        }

        for (actor in actorRegistry.values) {
            actor.start()
        }
    }

    /**
     * Stops all actors managed by this supervisor.
     *
     * @throws IllegalStateException if the supervisor is not in the Running state.
     */
    suspend fun stopAllActors() = registryMutex.withLock {
        if (state != ActorState.Running) {
            throw IllegalStateException("Cannot stop actors while supervisor is in state: $state")
        }

        for (actor in actorRegistry.values) {
            actor.stop()
        }
    }

    /**
     * Disposes all actors managed by this supervisor.
     */
    override suspend fun dispose() {
        registryMutex.withLock {
            for (actor in actorRegistry.values) {
                actor.dispose()
            }
            actorRegistry.clear()
            actorTypeRegistry.clear()
        }

        super.dispose()
    }
}
