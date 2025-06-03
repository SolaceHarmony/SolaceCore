package ai.solace.core.storage

import ai.solace.core.actor.ActorState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * File-based implementation of the ActorStateStorage interface.
 *
 * This implementation stores actor state data in files, with each actor's state stored in a separate file.
 * It extends the FileStorage class and implements the ActorStateStorage interface.
 *
 * @param baseDirectory The base directory where actor state data will be stored.
 */
class FileActorStateStorage(
    baseDirectory: String
) : FileStorage<String, Map<String, Any>>(
    baseDirectory = baseDirectory,
    keySerializer = { it },
    valueSerializer = { it },
    valueDeserializer = { it }
), ActorStateStorage {
    /**
     * Gets the state of an actor.
     *
     * @param actorId The ID of the actor.
     * @return The actor state, or null if the actor doesn't exist.
     */
    override suspend fun getActorState(actorId: String): ActorState? {
        val actorData = retrieve(actorId)?.first ?: return null
        val stateData = actorData["state"] as? Map<String, Any> ?: return null
        
        return when (stateData["type"]) {
            "Initialized" -> ActorState.Initialized
            "Running" -> ActorState.Running
            "Stopped" -> ActorState.Stopped
            "Error" -> ActorState.Error(stateData["exception"] as? String ?: "Unknown error")
            "Paused" -> ActorState.Paused(stateData["reason"] as? String ?: "Unknown reason")
            else -> null
        }
    }

    /**
     * Sets the state of an actor.
     *
     * @param actorId The ID of the actor.
     * @param state The actor state to set.
     * @return True if the state was set successfully, false otherwise.
     */
    override suspend fun setActorState(actorId: String, state: ActorState): Boolean {
        // Retrieve the data
        val retrievedData = retrieve(actorId)
        val actorData = retrievedData?.first?.toMutableMap() ?: mutableMapOf()
        val metadata = retrievedData?.second?.toMutableMap() ?: mutableMapOf()

        // Create state data
        val stateData = when (state) {
            is ActorState.Initialized -> mapOf("type" to "Initialized")
            is ActorState.Running -> mapOf("type" to "Running")
            is ActorState.Stopped -> mapOf("type" to "Stopped")
            is ActorState.Error -> mapOf("type" to "Error", "exception" to state.exception)
            is ActorState.Paused -> mapOf("type" to "Paused", "reason" to state.reason)
        }

        // Set state in actor data
        actorData["state"] = stateData

        // Store the updated data
        return store(actorId, actorData, metadata)
    }

    /**
     * Gets the port configurations for an actor.
     *
     * @param actorId The ID of the actor.
     * @return A map of port names to port configurations, or null if the actor doesn't exist.
     */
    override suspend fun getActorPorts(actorId: String): Map<String, Map<String, Any>>? {
        val actorData = retrieve(actorId)?.first ?: return null
        @Suppress("UNCHECKED_CAST")
        return actorData["ports"] as? Map<String, Map<String, Any>>
    }

    /**
     * Sets the port configurations for an actor.
     *
     * @param actorId The ID of the actor.
     * @param ports A map of port names to port configurations.
     * @return True if the port configurations were set successfully, false otherwise.
     */
    override suspend fun setActorPorts(actorId: String, ports: Map<String, Map<String, Any>>): Boolean {
        // Retrieve the data
        val retrievedData = retrieve(actorId)
        val actorData = retrievedData?.first?.toMutableMap() ?: mutableMapOf()
        val metadata = retrievedData?.second?.toMutableMap() ?: mutableMapOf()

        // Set ports in actor data
        actorData["ports"] = ports

        // Store the updated data
        return store(actorId, actorData, metadata)
    }

    /**
     * Gets the metrics for an actor.
     *
     * @param actorId The ID of the actor.
     * @return A map of metric names to metric values, or null if the actor doesn't exist.
     */
    override suspend fun getActorMetrics(actorId: String): Map<String, Any>? {
        val actorData = retrieve(actorId)?.first ?: return null
        @Suppress("UNCHECKED_CAST")
        return actorData["metrics"] as? Map<String, Any>
    }

    /**
     * Sets the metrics for an actor.
     *
     * @param actorId The ID of the actor.
     * @param metrics A map of metric names to metric values.
     * @return True if the metrics were set successfully, false otherwise.
     */
    override suspend fun setActorMetrics(actorId: String, metrics: Map<String, Any>): Boolean {
        // Retrieve the data
        val retrievedData = retrieve(actorId)
        val actorData = retrievedData?.first?.toMutableMap() ?: mutableMapOf()
        val metadata = retrievedData?.second?.toMutableMap() ?: mutableMapOf()

        // Set metrics in actor data
        actorData["metrics"] = metrics

        // Store the updated data
        return store(actorId, actorData, metadata)
    }

    /**
     * Gets the custom state data for an actor.
     *
     * @param actorId The ID of the actor.
     * @return A map of custom state data, or null if the actor doesn't exist.
     */
    override suspend fun getActorCustomState(actorId: String): Map<String, Any>? {
        val actorData = retrieve(actorId)?.first ?: return null
        @Suppress("UNCHECKED_CAST")
        return actorData["customState"] as? Map<String, Any>
    }

    /**
     * Sets the custom state data for an actor.
     *
     * @param actorId The ID of the actor.
     * @param customState A map of custom state data.
     * @return True if the custom state data was set successfully, false otherwise.
     */
    override suspend fun setActorCustomState(actorId: String, customState: Map<String, Any>): Boolean {
        // Retrieve the data
        val retrievedData = retrieve(actorId)
        val actorData = retrievedData?.first?.toMutableMap() ?: mutableMapOf()
        val metadata = retrievedData?.second?.toMutableMap() ?: mutableMapOf()

        // Set custom state in actor data
        actorData["customState"] = customState

        // Store the updated data
        return store(actorId, actorData, metadata)
    }
}