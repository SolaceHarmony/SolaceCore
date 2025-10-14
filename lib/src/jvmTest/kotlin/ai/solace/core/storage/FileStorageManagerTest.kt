package ai.solace.core.storage

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class FileStorageManagerTest {

    private lateinit var tempDir: String
    private lateinit var storageManager: FileStorageManager

    @BeforeTest
    fun setup() {
        // Create a temporary directory for testing
        tempDir = Files.createTempDirectory("file-storage-manager-test").toString()
        
        // Create a FileStorageManager instance
        storageManager = FileStorageManager(tempDir)
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
    fun testGetConfigurationStorage() = runBlocking {
        withTimeout(5.seconds) {
            // Get the configuration storage
            val configStorage = storageManager.getConfigurationStorage()
            
            // Verify that it's a FileConfigurationStorage instance
            assertTrue(configStorage is FileConfigurationStorage)
            
            // Test basic functionality
            val key = "test-key"
            val path = "test-path"
            val value = "test-value"
            
            configStorage.setConfigValue(key, path, value)
            assertEquals(value, configStorage.getConfigValue(key, path))
        }
    }

    @Test
    fun testGetActorStateStorage() = runBlocking {
        withTimeout(5.seconds) {
            // Get the actor state storage
            val actorStateStorage = storageManager.getActorStateStorage()
            
            // Verify that it's a FileActorStateStorage instance
            assertTrue(actorStateStorage is FileActorStateStorage)
            
            // Test basic functionality
            val actorId = "test-actor"
            val state = ai.solace.core.actor.ActorState.Running
            
            actorStateStorage.setActorState(actorId, state)
            assertEquals(state, actorStateStorage.getActorState(actorId))
        }
    }

    @Test
    fun testRegisterAndGetStorage() = runBlocking {
        withTimeout(5.seconds) {
            // Create a custom storage implementation
            val storage = FileStorage<Int, String>(
                baseDirectory = tempDir,
                keySerializer = { it.toString() },
                valueSerializer = { mapOf("value" to it) },
                valueDeserializer = { it["value"] as String }
            )
            
            // Register the storage
            val registered = storageManager.registerStorage(Int::class, String::class, storage)
            assertTrue(registered)
            
            // Get the storage
            val retrievedStorage = storageManager.getStorage(Int::class, String::class)
            assertNotNull(retrievedStorage)
            
            // Test basic functionality
            val key = 123
            val value = "test-value"
            
            retrievedStorage.store(key, value)
            val retrievedValue = retrievedStorage.retrieve(key)
            assertNotNull(retrievedValue)
            assertEquals(value, retrievedValue.first)
        }
    }

    @Test
    fun testUnregisterStorage() = runBlocking {
        withTimeout(5.seconds) {
            // Create a custom storage implementation
            val storage = FileStorage<Int, String>(
                baseDirectory = tempDir,
                keySerializer = { it.toString() },
                valueSerializer = { mapOf("value" to it) },
                valueDeserializer = { it["value"] as String }
            )
            
            // Register the storage
            storageManager.registerStorage(Int::class, String::class, storage)
            
            // Verify that the storage is registered
            assertNotNull(storageManager.getStorage(Int::class, String::class))
            
            // Unregister the storage
            val unregistered = storageManager.unregisterStorage(Int::class, String::class)
            assertTrue(unregistered)
            
            // Verify that the storage is no longer registered
            assertNull(storageManager.getStorage(Int::class, String::class))
        }
    }

    @Test
    fun testFlushAll() = runBlocking {
        withTimeout(5.seconds) {
            // File-based storage implementations write changes immediately, so flushAll should always return true
            val result = storageManager.flushAll()
            assertTrue(result)
        }
    }

    @Test
    fun testClearAll() = runBlocking {
        withTimeout(5.seconds) {
            // Store some data
            val configStorage = storageManager.getConfigurationStorage()
            configStorage.setConfigValue("test-key", "test-path", "test-value")
            
            val actorStateStorage = storageManager.getActorStateStorage()
            actorStateStorage.setActorState("test-actor", ai.solace.core.actor.ActorState.Running)
            
            // Clear all data
            val result = storageManager.clearAll()
            assertTrue(result)
            
            // Verify that the data is cleared
            assertNull(configStorage.getConfigValue("test-key", "test-path"))
            assertNull(actorStateStorage.getActorState("test-actor"))
        }
    }

    @Test
    fun testLifecycle() = runBlocking {
        withTimeout(5.seconds) {
            // Initially, the storage manager should not be active
            assertFalse(storageManager.isActive())
            
            // Start the storage manager
            storageManager.start()
            
            // Verify that the storage manager is active
            assertTrue(storageManager.isActive())
            
            // Stop the storage manager
            storageManager.stop()
            
            // Verify that the storage manager is not active
            assertFalse(storageManager.isActive())
            
            // Dispose of the storage manager
            storageManager.dispose()
            
            // Verify that the storage manager is not active
            assertFalse(storageManager.isActive())
        }
    }

    @Test
    fun testPersistence() = runBlocking {
        withTimeout(5.seconds) {
            // Store some data
            val configStorage = storageManager.getConfigurationStorage()
            configStorage.setConfigValue("test-key", "test-path", "test-value")
            
            // Create a new FileStorageManager instance with the same base directory
            val newStorageManager = FileStorageManager(tempDir)
            
            // Get the configuration storage from the new manager
            val newConfigStorage = newStorageManager.getConfigurationStorage()
            
            // Verify that the data is still available
            assertEquals("test-value", newConfigStorage.getConfigValue("test-key", "test-path"))
        }
    }
}
