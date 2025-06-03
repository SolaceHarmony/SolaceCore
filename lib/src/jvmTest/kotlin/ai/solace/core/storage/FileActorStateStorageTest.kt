package ai.solace.core.storage

import ai.solace.core.actor.ActorState
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class FileActorStateStorageTest {

    private lateinit var tempDir: String
    private lateinit var actorStateStorage: FileActorStateStorage

    @BeforeTest
    fun setup() {
        // Create a temporary directory for testing
        tempDir = Files.createTempDirectory("file-actor-state-test").toString()
        
        // Create a FileActorStateStorage instance
        actorStateStorage = FileActorStateStorage(tempDir)
    }

    @AfterTest
    fun teardown() {
        // Delete the temporary directory
        val path = Paths.get(tempDir)
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun testActorState() = runBlocking {
        withTimeout(5.seconds) {
            val actorId = "actor-1"

            // Test setting and getting Initialized state
            val initializedState = ActorState.Initialized
            assertTrue(actorStateStorage.setActorState(actorId, initializedState))
            assertEquals(initializedState, actorStateStorage.getActorState(actorId))

            // Test setting and getting Running state
            val runningState = ActorState.Running
            assertTrue(actorStateStorage.setActorState(actorId, runningState))
            assertEquals(runningState, actorStateStorage.getActorState(actorId))

            // Test setting and getting Stopped state
            val stoppedState = ActorState.Stopped
            assertTrue(actorStateStorage.setActorState(actorId, stoppedState))
            assertEquals(stoppedState, actorStateStorage.getActorState(actorId))

            // Test setting and getting Error state
            val errorState = ActorState.Error("Test error")
            assertTrue(actorStateStorage.setActorState(actorId, errorState))
            val retrievedErrorState = actorStateStorage.getActorState(actorId)
            assertTrue(retrievedErrorState is ActorState.Error)
            assertEquals("Test error", (retrievedErrorState as ActorState.Error).exception)

            // Test setting and getting Paused state
            val pausedState = ActorState.Paused("Test pause")
            assertTrue(actorStateStorage.setActorState(actorId, pausedState))
            val retrievedPausedState = actorStateStorage.getActorState(actorId)
            assertTrue(retrievedPausedState is ActorState.Paused)
            assertEquals("Test pause", (retrievedPausedState as ActorState.Paused).reason)
        }
    }

    @Test
    fun testActorPorts() = runBlocking {
        withTimeout(5.seconds) {
            val actorId = "actor-1"

            // Define port configurations
            val ports = mapOf(
                "input" to mapOf(
                    "type" to "String",
                    "bufferSize" to 10
                ),
                "output" to mapOf(
                    "type" to "Int",
                    "bufferSize" to 5
                )
            )

            // Set port configurations
            assertTrue(actorStateStorage.setActorPorts(actorId, ports))

            // Get port configurations
            val retrievedPorts = actorStateStorage.getActorPorts(actorId)
            assertNotNull(retrievedPorts)
            assertEquals(2, retrievedPorts.size)

            // Verify input port
            val inputPort = retrievedPorts["input"]
            assertNotNull(inputPort)
            assertEquals("String", inputPort["type"])
            assertEquals(10, inputPort["bufferSize"])

            // Verify output port
            val outputPort = retrievedPorts["output"]
            assertNotNull(outputPort)
            assertEquals("Int", outputPort["type"])
            assertEquals(5, outputPort["bufferSize"])
        }
    }

    @Test
    fun testActorMetrics() = runBlocking {
        withTimeout(5.seconds) {
            val actorId = "actor-1"

            // Define metrics
            val metrics = mapOf(
                "messagesProcessed" to 100L,
                "averageProcessingTime" to 25.5,
                "errors" to 2L
            )

            // Set metrics
            assertTrue(actorStateStorage.setActorMetrics(actorId, metrics))

            // Get metrics
            val retrievedMetrics = actorStateStorage.getActorMetrics(actorId)
            assertNotNull(retrievedMetrics)
            assertEquals(3, retrievedMetrics.size)
            assertEquals(100L, retrievedMetrics["messagesProcessed"])
            assertEquals(25.5, retrievedMetrics["averageProcessingTime"])
            assertEquals(2L, retrievedMetrics["errors"])
        }
    }

    @Test
    fun testActorCustomState() = runBlocking {
        withTimeout(5.seconds) {
            val actorId = "actor-1"

            // Define custom state
            val customState = mapOf(
                "lastProcessedId" to "msg-123",
                "configuration" to mapOf(
                    "retryCount" to 3L,  // Use Long instead of Int to avoid type mismatch
                    "timeout" to 1000L
                )
            )

            // Set custom state
            assertTrue(actorStateStorage.setActorCustomState(actorId, customState))

            // Get custom state
            val retrievedCustomState = actorStateStorage.getActorCustomState(actorId)
            assertNotNull(retrievedCustomState)
            assertEquals(2, retrievedCustomState.size)
            assertEquals("msg-123", retrievedCustomState["lastProcessedId"])

            // Verify nested map
            val configuration = retrievedCustomState["configuration"] as? Map<*, *>
            assertNotNull(configuration)
            assertEquals(3L, configuration["retryCount"])  // Expect Long instead of Int
            assertEquals(1000L, configuration["timeout"])
        }
    }

    @Test
    fun testNonExistentActor() = runBlocking {
        withTimeout(5.seconds) {
            val actorId = "non-existent-actor"

            // Try to get state for non-existent actor
            assertNull(actorStateStorage.getActorState(actorId))

            // Try to get ports for non-existent actor
            assertNull(actorStateStorage.getActorPorts(actorId))

            // Try to get metrics for non-existent actor
            assertNull(actorStateStorage.getActorMetrics(actorId))

            // Try to get custom state for non-existent actor
            assertNull(actorStateStorage.getActorCustomState(actorId))
        }
    }

    @Test
    fun testDeleteActor() = runBlocking {
        withTimeout(5.seconds) {
            val actorId = "actor-to-delete"

            // Set actor state
            assertTrue(actorStateStorage.setActorState(actorId, ActorState.Running))

            // Verify actor exists
            assertNotNull(actorStateStorage.getActorState(actorId))

            // Delete actor
            assertTrue(actorStateStorage.delete(actorId))

            // Verify actor no longer exists
            assertNull(actorStateStorage.getActorState(actorId))
        }
    }

    @Test
    fun testPersistence() = runBlocking {
        withTimeout(5.seconds) {
            val actorId = "actor-1"

            // Set actor state
            assertTrue(actorStateStorage.setActorState(actorId, ActorState.Running))
            
            // Create a new FileActorStateStorage instance with the same base directory
            val newActorStateStorage = FileActorStateStorage(tempDir)
            
            // Verify that the actor state is still available
            assertEquals(ActorState.Running, newActorStateStorage.getActorState(actorId))
        }
    }
}