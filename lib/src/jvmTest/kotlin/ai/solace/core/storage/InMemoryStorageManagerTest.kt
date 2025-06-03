package ai.solace.core.storage

import kotlinx.coroutines.runBlocking
import kotlin.test.*

class InMemoryStorageManagerTest {
    
    private lateinit var storageManager: InMemoryStorageManager
    
    @BeforeTest
    fun setup() {
        storageManager = InMemoryStorageManager()
    }
    
    @Test
    fun testLifecycle() = runBlocking {
        // Initially, the storage manager should not be active
        assertFalse(storageManager.isActive())
        
        // Start the storage manager
        storageManager.start()
        assertTrue(storageManager.isActive())
        
        // Stop the storage manager
        storageManager.stop()
        assertFalse(storageManager.isActive())
        
        // Start again
        storageManager.start()
        assertTrue(storageManager.isActive())
        
        // Dispose
        storageManager.dispose()
        assertFalse(storageManager.isActive())
    }
    
    @Test
    fun testGetConfigurationStorage() = runBlocking {
        // Get the configuration storage
        val configStorage = storageManager.getConfigurationStorage()
        assertNotNull(configStorage)
        assertTrue(configStorage is InMemoryConfigurationStorage)
        
        // Test basic functionality
        val key = "test"
        val path = "value"
        val value = "test-value"
        
        configStorage.setConfigValue(key, path, value)
        assertEquals(value, configStorage.getConfigValue(key, path))
    }
    
    @Test
    fun testGetActorStateStorage() = runBlocking {
        // Get the actor state storage
        val actorStateStorage = storageManager.getActorStateStorage()
        assertNotNull(actorStateStorage)
        assertTrue(actorStateStorage is InMemoryActorStateStorage)
        
        // Test basic functionality
        val actorId = "test-actor"
        val state = ai.solace.core.actor.ActorState.Running
        
        actorStateStorage.setActorState(actorId, state)
        assertEquals(state, actorStateStorage.getActorState(actorId))
    }
    
    @Test
    fun testRegisterAndGetStorage() = runBlocking {
        // Create a custom storage implementation
        val customStorage = InMemoryStorage<Int, String>()
        
        // Register the storage
        val registerResult = storageManager.registerStorage(
            Int::class.java,
            String::class.java,
            customStorage,
            "custom"
        )
        assertTrue(registerResult)
        
        // Get the storage
        val retrievedStorage = storageManager.getStorage(
            Int::class.java,
            String::class.java,
            "custom"
        )
        assertNotNull(retrievedStorage)
        assertTrue(retrievedStorage is InMemoryStorage<*, *>)
        
        // Test basic functionality
        val key = 42
        val value = "answer"
        
        retrievedStorage.store(key, value)
        val retrievedValue = retrievedStorage.retrieve(key)?.first
        assertEquals(value, retrievedValue)
    }
    
    @Test
    fun testUnregisterStorage() = runBlocking {
        // Create and register a custom storage implementation
        val customStorage = InMemoryStorage<Int, String>()
        storageManager.registerStorage(
            Int::class.java,
            String::class.java,
            customStorage,
            "custom"
        )
        
        // Verify it's registered
        assertNotNull(storageManager.getStorage(Int::class.java, String::class.java, "custom"))
        
        // Unregister the storage
        val unregisterResult = storageManager.unregisterStorage(
            Int::class.java,
            String::class.java,
            "custom"
        )
        assertTrue(unregisterResult)
        
        // Verify it's no longer registered
        assertNull(storageManager.getStorage(Int::class.java, String::class.java, "custom"))
    }
    
    @Test
    fun testClearAll() = runBlocking {
        // Add some data to configuration storage
        val configStorage = storageManager.getConfigurationStorage()
        configStorage.setConfigValue("test", "value", "test-value")
        
        // Add some data to actor state storage
        val actorStateStorage = storageManager.getActorStateStorage()
        actorStateStorage.setActorState("test-actor", ai.solace.core.actor.ActorState.Running)
        
        // Register a custom storage and add data
        val customStorage = InMemoryStorage<String, Int>()
        storageManager.registerStorage(String::class.java, Int::class.java, customStorage)
        customStorage.store("test", 42)
        
        // Clear all data
        val clearResult = storageManager.clearAll()
        assertTrue(clearResult)
        
        // Verify all data is cleared
        assertNull(configStorage.getConfigValue("test", "value"))
        assertNull(actorStateStorage.getActorState("test-actor"))
        assertNull(customStorage.retrieve("test"))
    }
    
    @Test
    fun testFlushAll() = runBlocking {
        // In-memory storage doesn't need to be flushed, so this should always return true
        val flushResult = storageManager.flushAll()
        assertTrue(flushResult)
    }
    
    @Test
    fun testNonExistentStorage() = runBlocking {
        // Try to get a non-existent storage
        val storage = storageManager.getStorage(
            Long::class.java,
            Boolean::class.java,
            "non-existent"
        )
        assertNull(storage)
        
        // Try to unregister a non-existent storage
        val unregisterResult = storageManager.unregisterStorage(
            Long::class.java,
            Boolean::class.java,
            "non-existent"
        )
        assertFalse(unregisterResult)
    }
}