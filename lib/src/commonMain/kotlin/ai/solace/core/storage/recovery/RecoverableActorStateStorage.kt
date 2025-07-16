package ai.solace.core.storage.recovery

import ai.solace.core.storage.ActorStateStorage

/**
 * Extension of ActorStateStorage interface to support state recovery.
 *
 * This interface adds methods for creating, restoring, and managing snapshots
 * of actor state, which are used for recovery after system restarts or crashes.
 */
interface RecoverableActorStateStorage : ActorStateStorage {
    /**
     * Creates a snapshot of an actor's state.
     *
     * @param actorId The ID of the actor.
     * @return The snapshot of the actor's state, or null if the actor doesn't exist.
     */
    suspend fun createSnapshot(actorId: String): ActorStateSnapshot?

    /**
     * Restores an actor's state from a snapshot.
     *
     * @param snapshot The snapshot to restore from.
     * @return True if the state was restored successfully, false otherwise.
     */
    suspend fun restoreFromSnapshot(snapshot: ActorStateSnapshot): Boolean

    /**
     * Lists all available snapshots for an actor.
     *
     * @param actorId The ID of the actor.
     * @return A list of snapshots for the actor, sorted by timestamp in descending order.
     */
    suspend fun listSnapshots(actorId: String): List<ActorStateSnapshot>

    /**
     * Gets the latest snapshot for an actor.
     *
     * @param actorId The ID of the actor.
     * @return The latest snapshot for the actor, or null if no snapshots exist.
     */
    suspend fun getLatestSnapshot(actorId: String): ActorStateSnapshot? {
        val snapshots = listSnapshots(actorId)
        return snapshots.maxByOrNull { it.timestamp }
    }

    /**
     * Gets a snapshot for an actor by version.
     *
     * @param actorId The ID of the actor.
     * @param version The version of the snapshot to get.
     * @return The snapshot with the specified version, or null if no such snapshot exists.
     */
    suspend fun getSnapshotByVersion(actorId: String, version: Int): ActorStateSnapshot? {
        val snapshots = listSnapshots(actorId)
        return snapshots.find { it.version == version }
    }

    /**
     * Deletes a snapshot for an actor.
     *
     * @param actorId The ID of the actor.
     * @param version The version of the snapshot to delete.
     * @return True if the snapshot was deleted successfully, false otherwise.
     */
    suspend fun deleteSnapshot(actorId: String, version: Int): Boolean

    /**
     * Deletes all snapshots for an actor.
     *
     * @param actorId The ID of the actor.
     * @return True if all snapshots were deleted successfully, false otherwise.
     */
    suspend fun deleteAllSnapshots(actorId: String): Boolean
}