package ai.solace.core.storage.recovery

import ai.solace.core.actor.Actor
import ai.solace.core.actor.ActorState
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class ActorRecoveryManagerTest {

    private lateinit var storage: MockRecoverableActorStateStorage
    private lateinit var recoveryManager: ActorRecoveryManager

    @BeforeTest
    fun setup() {
        storage = MockRecoverableActorStateStorage()
        recoveryManager = ActorRecoveryManager(storage)
    }

    @Test
    fun testCreateSnapshot() {
        runBlocking {
            // Create a mock actor
            val actor = MockActor("test-actor", "Test Actor")

            // Create a snapshot
            val snapshot = recoveryManager.createSnapshot(actor)

            // Verify that the snapshot was created
            assertNotNull(snapshot)
            assertEquals("test-actor", snapshot.actorId)
            assertEquals("Test Actor", snapshot.actorName)
            assertEquals(ActorState.Initialized, snapshot.state)
            assertEquals(1, snapshot.version)
        }
    }

    @Test
    fun testRestoreActor() {
        runBlocking {
            // Create a snapshot
            val snapshot = ActorStateSnapshot.builder("test-actor")
                .withName("Test Actor")
                .withState(ActorState.Running)
                .withPorts(mapOf("input" to mapOf("type" to "String")))
                .withMetrics(mapOf("count" to 42))
                .withCustomState(mapOf("key" to "value"))
                .withVersion(1)
                .withTimestamp(System.currentTimeMillis())
                .build()

            // Store the snapshot
            storage.restoreFromSnapshot(snapshot)

            // Restore the actor
            val actor = recoveryManager.restoreActor(snapshot) { id, name ->
                MockActor(id, name)
            }

            // Verify that the actor was restored
            assertNotNull(actor)
            assertEquals("test-actor", actor.id)
            assertEquals("Test Actor", actor.name)
            assertEquals(ActorState.Running, actor.state)
        }
    }

    @Test
    fun testRecoverAllActors() {
        runBlocking {
            // Create snapshots for multiple actors
            val snapshot1 = ActorStateSnapshot.builder("actor1")
                .withName("Actor 1")
                .withState(ActorState.Running)
                .withVersion(1)
                .withTimestamp(System.currentTimeMillis())
                .build()

            val snapshot2 = ActorStateSnapshot.builder("actor2")
                .withName("Actor 2")
                .withState(ActorState.Stopped)
                .withVersion(1)
                .withTimestamp(System.currentTimeMillis())
                .build()

            // Store the snapshots
            storage.restoreFromSnapshot(snapshot1)
            storage.restoreFromSnapshot(snapshot2)

            // Add the actor IDs to the list of keys
            storage.addKey("actor1")
            storage.addKey("actor2")

            // Recover all actors
            val actors = recoveryManager.recoverAllActors { id, name ->
                MockActor(id, name)
            }

            // Verify that all actors were recovered
            assertEquals(2, actors.size)
            assertTrue(actors.containsKey("actor1"))
            assertTrue(actors.containsKey("actor2"))
            assertEquals("Actor 1", actors["actor1"]?.name)
            assertEquals("Actor 2", actors["actor2"]?.name)
            assertEquals(ActorState.Running, actors["actor1"]?.state)
            assertEquals(ActorState.Stopped, actors["actor2"]?.state)
        }
    }

    @Test
    fun testGetLatestSnapshot() {
        runBlocking {
            // Create multiple snapshots for the same actor
            val snapshot1 = ActorStateSnapshot.builder("test-actor")
                .withName("Test Actor")
                .withState(ActorState.Initialized)
                .withVersion(1)
                .withTimestamp(1000)
                .build()

            val snapshot2 = ActorStateSnapshot.builder("test-actor")
                .withName("Test Actor")
                .withState(ActorState.Running)
                .withVersion(2)
                .withTimestamp(2000)
                .build()

            // Store the snapshots
            storage.addSnapshot(snapshot1)
            storage.addSnapshot(snapshot2)

            // Get the latest snapshot
            val latestSnapshot = recoveryManager.getLatestSnapshot("test-actor")

            // Verify that the latest snapshot was returned
            assertNotNull(latestSnapshot)
            assertEquals(2, latestSnapshot.version)
            assertEquals(ActorState.Running, latestSnapshot.state)
        }
    }

    @Test
    fun testGetSnapshotByVersion() {
        runBlocking {
            // Create multiple snapshots for the same actor
            val snapshot1 = ActorStateSnapshot.builder("test-actor")
                .withName("Test Actor")
                .withState(ActorState.Initialized)
                .withVersion(1)
                .withTimestamp(1000)
                .build()

            val snapshot2 = ActorStateSnapshot.builder("test-actor")
                .withName("Test Actor")
                .withState(ActorState.Running)
                .withVersion(2)
                .withTimestamp(2000)
                .build()

            // Store the snapshots
            storage.addSnapshot(snapshot1)
            storage.addSnapshot(snapshot2)

            // Get the snapshot by version
            val snapshotV1 = recoveryManager.getSnapshotByVersion("test-actor", 1)

            // Verify that the correct snapshot was returned
            assertNotNull(snapshotV1)
            assertEquals(1, snapshotV1.version)
            assertEquals(ActorState.Initialized, snapshotV1.state)
        }
    }

    @Test
    fun testListSnapshots() {
        runBlocking {
            // Create multiple snapshots for the same actor
            val snapshot1 = ActorStateSnapshot.builder("test-actor")
                .withName("Test Actor")
                .withState(ActorState.Initialized)
                .withVersion(1)
                .withTimestamp(1000)
                .build()

            val snapshot2 = ActorStateSnapshot.builder("test-actor")
                .withName("Test Actor")
                .withState(ActorState.Running)
                .withVersion(2)
                .withTimestamp(2000)
                .build()

            // Store the snapshots
            storage.addSnapshot(snapshot1)
            storage.addSnapshot(snapshot2)

            // List all snapshots
            val snapshots = recoveryManager.listSnapshots("test-actor")

            // Verify that all snapshots were returned
            assertEquals(2, snapshots.size)
            assertEquals(1, snapshots[0].version)
            assertEquals(2, snapshots[1].version)
        }
    }

    @Test
    fun testDeleteSnapshot() {
        runBlocking {
            // Create a snapshot
            val snapshot = ActorStateSnapshot.builder("test-actor")
                .withName("Test Actor")
                .withState(ActorState.Running)
                .withVersion(1)
                .withTimestamp(System.currentTimeMillis())
                .build()

            // Store the snapshot
            storage.addSnapshot(snapshot)

            // Delete the snapshot
            val result = recoveryManager.deleteSnapshot("test-actor", 1)

            // Verify that the snapshot was deleted
            assertTrue(result)
            assertTrue(storage.snapshots.isEmpty())
        }
    }

    @Test
    fun testDeleteAllSnapshots() {
        runBlocking {
            // Create multiple snapshots for the same actor
            val snapshot1 = ActorStateSnapshot.builder("test-actor")
                .withName("Test Actor")
                .withState(ActorState.Initialized)
                .withVersion(1)
                .withTimestamp(1000)
                .build()

            val snapshot2 = ActorStateSnapshot.builder("test-actor")
                .withName("Test Actor")
                .withState(ActorState.Running)
                .withVersion(2)
                .withTimestamp(2000)
                .build()

            // Store the snapshots
            storage.addSnapshot(snapshot1)
            storage.addSnapshot(snapshot2)

            // Delete all snapshots
            val result = recoveryManager.deleteAllSnapshots("test-actor")

            // Verify that all snapshots were deleted
            assertTrue(result)
            assertTrue(storage.snapshots.isEmpty())
        }
    }

    /**
     * Mock implementation of Actor for testing.
     */
    private class MockActor(
        id: String,
        name: String
    ) : Actor(id, name) {
        override suspend fun start() {
            super.start()
        }

        override suspend fun stop() {
            super.stop()
        }

        // We can't directly access the _state field, so we'll use a different approach
        // The ActorRecoveryManager will call this method when restoring from a snapshot
        // with a Paused state
    }

    /**
     * Mock implementation of RecoverableActorStateStorage for testing.
     */
    private class MockRecoverableActorStateStorage : RecoverableActorStateStorage {
        val snapshots = mutableListOf<ActorStateSnapshot>()
        val keys = mutableListOf<String>()
        val actorData = mutableMapOf<String, MutableMap<String, Any>>()

        fun addSnapshot(snapshot: ActorStateSnapshot) {
            snapshots.add(snapshot)
        }

        fun addKey(key: String) {
            keys.add(key)
        }

        override suspend fun createSnapshot(actorId: String): ActorStateSnapshot? {
            val actorData = actorData[actorId] ?: return null
            val version = snapshots.filter { it.actorId == actorId }.maxOfOrNull { it.version } ?: 0

            return ActorStateSnapshot.builder(actorId)
                .withName(actorData["name"] as? String ?: "Unknown")
                .withState(actorData["state"] as? ActorState ?: ActorState.Initialized)
                .withPorts(actorData["ports"] as? Map<String, Map<String, Any>> ?: emptyMap())
                .withMetrics(actorData["metrics"] as? Map<String, Any> ?: emptyMap())
                .withCustomState(actorData["customState"] as? Map<String, Any> ?: emptyMap())
                .withVersion(version + 1)
                .withTimestamp(System.currentTimeMillis())
                .build()
        }

        override suspend fun restoreFromSnapshot(snapshot: ActorStateSnapshot): Boolean {
            addSnapshot(snapshot)

            val data = actorData.getOrPut(snapshot.actorId) { mutableMapOf() }
            data["name"] = snapshot.actorName
            data["state"] = snapshot.state
            data["ports"] = snapshot.ports
            data["metrics"] = snapshot.metrics
            data["customState"] = snapshot.customState

            return true
        }

        override suspend fun listSnapshots(actorId: String): List<ActorStateSnapshot> {
            return snapshots.filter { it.actorId == actorId }
        }

        override suspend fun deleteSnapshot(actorId: String, version: Int): Boolean {
            val index = snapshots.indexOfFirst { it.actorId == actorId && it.version == version }
            if (index >= 0) {
                snapshots.removeAt(index)
                return true
            }
            return false
        }

        override suspend fun deleteAllSnapshots(actorId: String): Boolean {
            snapshots.removeAll { it.actorId == actorId }
            return true
        }

        override suspend fun getActorState(actorId: String): ActorState? {
            return actorData[actorId]?.get("state") as? ActorState
        }

        override suspend fun setActorState(actorId: String, state: ActorState): Boolean {
            val data = actorData.getOrPut(actorId) { mutableMapOf() }
            data["state"] = state
            return true
        }

        override suspend fun getActorPorts(actorId: String): Map<String, Map<String, Any>>? {
            return actorData[actorId]?.get("ports") as? Map<String, Map<String, Any>>
        }

        override suspend fun setActorPorts(actorId: String, ports: Map<String, Map<String, Any>>): Boolean {
            val data = actorData.getOrPut(actorId) { mutableMapOf() }
            data["ports"] = ports
            return true
        }

        override suspend fun getActorMetrics(actorId: String): Map<String, Any>? {
            return actorData[actorId]?.get("metrics") as? Map<String, Any>
        }

        override suspend fun setActorMetrics(actorId: String, metrics: Map<String, Any>): Boolean {
            val data = actorData.getOrPut(actorId) { mutableMapOf() }
            data["metrics"] = metrics
            return true
        }

        override suspend fun getActorCustomState(actorId: String): Map<String, Any>? {
            return actorData[actorId]?.get("customState") as? Map<String, Any>
        }

        override suspend fun setActorCustomState(actorId: String, customState: Map<String, Any>): Boolean {
            val data = actorData.getOrPut(actorId) { mutableMapOf() }
            data["customState"] = customState
            return true
        }

        override suspend fun store(key: String, value: Map<String, Any>, metadata: Map<String, Any>): Boolean {
            actorData[key] = value.toMutableMap()
            return true
        }

        override suspend fun retrieve(key: String): Pair<Map<String, Any>, Map<String, Any>>? {
            val data = actorData[key] ?: return null
            return Pair(data, emptyMap())
        }

        override suspend fun listKeys(): List<String> {
            return keys
        }

        override suspend fun delete(key: String): Boolean {
            actorData.remove(key)
            keys.remove(key)
            return true
        }

        override suspend fun exists(key: String): Boolean {
            return actorData.containsKey(key)
        }

        override suspend fun updateMetadata(key: String, metadata: Map<String, Any>): Boolean {
            return true
        }
    }
}
