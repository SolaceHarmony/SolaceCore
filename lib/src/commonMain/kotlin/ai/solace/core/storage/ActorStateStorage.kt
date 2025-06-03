package ai.solace.core.storage

import ai.solace.core.actor.ActorState

/**
 * Interface for storing and retrieving actor state data.
 *
 * This interface extends the generic Storage interface with actor state-specific functionality.
 * It is designed to handle state data for actors in the system.
 */
interface ActorStateStorage : Storage<String, Map<String, Any>> {
    /**
     * Gets the state of an actor.
     *
     * @param actorId The ID of the actor.
     * @return The actor state, or null if the actor doesn't exist.
     */
    suspend fun getActorState(actorId: String): ActorState?

    /**
     * Sets the state of an actor.
     *
     * @param actorId The ID of the actor.
     * @param state The actor state to set.
     * @return True if the state was set successfully, false otherwise.
     */
    suspend fun setActorState(actorId: String, state: ActorState): Boolean

    /**
     * Gets the port configurations for an actor.
     *
     * @param actorId The ID of the actor.
     * @return A map of port names to port configurations, or null if the actor doesn't exist.
     */
    suspend fun getActorPorts(actorId: String): Map<String, Map<String, Any>>?

    /**
     * Sets the port configurations for an actor.
     *
     * @param actorId The ID of the actor.
     * @param ports A map of port names to port configurations.
     * @return True if the port configurations were set successfully, false otherwise.
     */
    suspend fun setActorPorts(actorId: String, ports: Map<String, Map<String, Any>>): Boolean

    /**
     * Gets the metrics for an actor.
     *
     * @param actorId The ID of the actor.
     * @return A map of metric names to metric values, or null if the actor doesn't exist.
     */
    suspend fun getActorMetrics(actorId: String): Map<String, Any>?

    /**
     * Sets the metrics for an actor.
     *
     * @param actorId The ID of the actor.
     * @param metrics A map of metric names to metric values.
     * @return True if the metrics were set successfully, false otherwise.
     */
    suspend fun setActorMetrics(actorId: String, metrics: Map<String, Any>): Boolean

    /**
     * Gets the custom state data for an actor.
     *
     * @param actorId The ID of the actor.
     * @return A map of custom state data, or null if the actor doesn't exist.
     */
    suspend fun getActorCustomState(actorId: String): Map<String, Any>?

    /**
     * Sets the custom state data for an actor.
     *
     * @param actorId The ID of the actor.
     * @param customState A map of custom state data.
     * @return True if the custom state data was set successfully, false otherwise.
     */
    suspend fun setActorCustomState(actorId: String, customState: Map<String, Any>): Boolean
}