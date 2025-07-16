package ai.solace.core.storage.recovery

import ai.solace.core.actor.Actor
import ai.solace.core.actor.ActorState

/**
 * Manager for handling actor recovery.
 *
 * This class provides a higher-level API for managing actor recovery,
 * including creating snapshots, restoring actors from snapshots, and
 * recovering all actors from storage.
 *
 * @param storage The recoverable actor state storage.
 */
class ActorRecoveryManager(
    private val storage: RecoverableActorStateStorage
) {
    /**
     * Creates a snapshot of an actor's state.
     *
     * @param actor The actor to create a snapshot for.
     * @return The created snapshot, or null if snapshot creation failed.
     */
    suspend fun createSnapshot(actor: Actor): ActorStateSnapshot? {
        // Get the actor's state data
        val actorId = actor.id
        val actorName = actor.name
        val state = actor.state
        val ports = storage.getActorPorts(actorId) ?: emptyMap()
        val metrics = storage.getActorMetrics(actorId) ?: emptyMap()
        val customState = storage.getActorCustomState(actorId) ?: emptyMap()
        
        // Get the current version
        val currentSnapshot = storage.getLatestSnapshot(actorId)
        val version = currentSnapshot?.version?.plus(1) ?: 1
        
        // Create the snapshot
        val snapshot = ActorStateSnapshot.builder(actorId)
            .withName(actorName)
            .withState(state)
            .withPorts(ports)
            .withMetrics(metrics)
            .withCustomState(customState)
            .withVersion(version)
            .withTimestamp(System.currentTimeMillis())
            .build()
        
        // Store the snapshot
        return if (storage.restoreFromSnapshot(snapshot)) {
            snapshot
        } else {
            null
        }
    }
    
    /**
     * Restores an actor from a snapshot.
     *
     * @param snapshot The snapshot to restore from.
     * @param actorFactory A function that creates a new actor instance with the given ID and name.
     * @return The restored actor, or null if restoration failed.
     */
    suspend fun restoreActor(
        snapshot: ActorStateSnapshot,
        actorFactory: (String, String) -> Actor
    ): Actor? {
        // Restore the snapshot to storage
        if (!storage.restoreFromSnapshot(snapshot)) {
            return null
        }
        
        // Create a new actor instance
        val actor = actorFactory(snapshot.actorId, snapshot.actorName)
        
        // Set the actor's state
        when (snapshot.state) {
            is ActorState.Running -> actor.start()
            is ActorState.Stopped -> actor.stop()
            is ActorState.Paused -> {
                actor.start()
                if (snapshot.state is ActorState.Paused) {
                    actor.pause(snapshot.state.reason)
                }
            }
            is ActorState.Error -> {
                // For error state, we just leave the actor in the initialized state
                // and let the application handle the error
            }
            else -> {
                // For other states, we just leave the actor in the initialized state
            }
        }
        
        return actor
    }
    
    /**
     * Recovers all actors from storage.
     *
     * @param actorFactory A function that creates a new actor instance with the given ID and name.
     * @return A map of actor IDs to restored actors.
     */
    suspend fun recoverAllActors(
        actorFactory: (String, String) -> Actor
    ): Map<String, Actor> {
        val result = mutableMapOf<String, Actor>()
        
        // Get all actor IDs
        val actorIds = storage.listKeys()
        
        // Recover each actor
        for (actorId in actorIds) {
            val snapshot = storage.getLatestSnapshot(actorId) ?: continue
            val actor = restoreActor(snapshot, actorFactory) ?: continue
            result[actorId] = actor
        }
        
        return result
    }
    
    /**
     * Gets the latest snapshot for an actor.
     *
     * @param actorId The ID of the actor.
     * @return The latest snapshot for the actor, or null if no snapshots exist.
     */
    suspend fun getLatestSnapshot(actorId: String): ActorStateSnapshot? {
        return storage.getLatestSnapshot(actorId)
    }
    
    /**
     * Gets a snapshot for an actor by version.
     *
     * @param actorId The ID of the actor.
     * @param version The version of the snapshot to get.
     * @return The snapshot with the specified version, or null if no such snapshot exists.
     */
    suspend fun getSnapshotByVersion(actorId: String, version: Int): ActorStateSnapshot? {
        return storage.getSnapshotByVersion(actorId, version)
    }
    
    /**
     * Lists all available snapshots for an actor.
     *
     * @param actorId The ID of the actor.
     * @return A list of snapshots for the actor, sorted by timestamp in descending order.
     */
    suspend fun listSnapshots(actorId: String): List<ActorStateSnapshot> {
        return storage.listSnapshots(actorId)
    }
    
    /**
     * Deletes a snapshot for an actor.
     *
     * @param actorId The ID of the actor.
     * @param version The version of the snapshot to delete.
     * @return True if the snapshot was deleted successfully, false otherwise.
     */
    suspend fun deleteSnapshot(actorId: String, version: Int): Boolean {
        return storage.deleteSnapshot(actorId, version)
    }
    
    /**
     * Deletes all snapshots for an actor.
     *
     * @param actorId The ID of the actor.
     * @return True if all snapshots were deleted successfully, false otherwise.
     */
    suspend fun deleteAllSnapshots(actorId: String): Boolean {
        return storage.deleteAllSnapshots(actorId)
    }
}