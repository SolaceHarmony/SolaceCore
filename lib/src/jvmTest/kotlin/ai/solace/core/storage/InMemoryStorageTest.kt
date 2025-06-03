package ai.solace.core.storage

import kotlinx.coroutines.runBlocking
import kotlin.test.*

class InMemoryStorageTest {
    
    private lateinit var storage: InMemoryStorage<String, String>
    
    @BeforeTest
    fun setup() {
        storage = InMemoryStorage()
    }
    
    @Test
    fun testStoreAndRetrieve() = runBlocking {
        // Store a value
        val key = "test-key"
        val value = "test-value"
        val metadata = mapOf("timestamp" to 12345L)
        
        val storeResult = storage.store(key, value, metadata)
        assertTrue(storeResult, "Store operation should succeed")
        
        // Retrieve the value
        val retrievedData = storage.retrieve(key)
        assertNotNull(retrievedData, "Retrieved data should not be null")
        
        val (retrievedValue, retrievedMetadata) = retrievedData
        assertEquals(value, retrievedValue, "Retrieved value should match stored value")
        assertEquals(metadata["timestamp"], retrievedMetadata["timestamp"], "Retrieved metadata should match stored metadata")
    }
    
    @Test
    fun testListKeys() = runBlocking {
        // Store multiple values
        storage.store("key1", "value1")
        storage.store("key2", "value2")
        storage.store("key3", "value3")
        
        // List all keys
        val keys = storage.listKeys()
        assertEquals(3, keys.size, "Should have 3 keys")
        assertTrue(keys.contains("key1"), "Keys should contain key1")
        assertTrue(keys.contains("key2"), "Keys should contain key2")
        assertTrue(keys.contains("key3"), "Keys should contain key3")
    }
    
    @Test
    fun testDelete() = runBlocking {
        // Store a value
        storage.store("key-to-delete", "value")
        
        // Verify it exists
        assertTrue(storage.exists("key-to-delete"), "Key should exist before deletion")
        
        // Delete the value
        val deleteResult = storage.delete("key-to-delete")
        assertTrue(deleteResult, "Delete operation should succeed")
        
        // Verify it no longer exists
        assertFalse(storage.exists("key-to-delete"), "Key should not exist after deletion")
        assertNull(storage.retrieve("key-to-delete"), "Retrieve should return null after deletion")
    }
    
    @Test
    fun testUpdateMetadata() = runBlocking {
        // Store a value with metadata
        val key = "metadata-key"
        val value = "metadata-value"
        val initialMetadata = mapOf("initial" to "value")
        
        storage.store(key, value, initialMetadata)
        
        // Update the metadata
        val updatedMetadata = mapOf("updated" to "new-value")
        val updateResult = storage.updateMetadata(key, updatedMetadata)
        
        assertTrue(updateResult, "Metadata update should succeed")
        
        // Retrieve and verify the updated metadata
        val retrievedData = storage.retrieve(key)
        assertNotNull(retrievedData, "Retrieved data should not be null")
        
        val (retrievedValue, retrievedMetadata) = retrievedData
        assertEquals(value, retrievedValue, "Value should remain unchanged")
        assertEquals(updatedMetadata["updated"], retrievedMetadata["updated"], "Metadata should be updated")
        assertNull(retrievedMetadata["initial"], "Old metadata should be replaced")
    }
    
    @Test
    fun testClear() = runBlocking {
        // Store multiple values
        storage.store("key1", "value1")
        storage.store("key2", "value2")
        
        // Clear the storage
        val clearResult = storage.clear()
        assertTrue(clearResult, "Clear operation should succeed")
        
        // Verify all keys are gone
        val keys = storage.listKeys()
        assertTrue(keys.isEmpty(), "Storage should be empty after clear")
    }
    
    @Test
    fun testNonExistentKey() = runBlocking {
        // Try to retrieve a non-existent key
        val retrievedData = storage.retrieve("non-existent-key")
        assertNull(retrievedData, "Retrieving non-existent key should return null")
        
        // Check if a non-existent key exists
        assertFalse(storage.exists("non-existent-key"), "Non-existent key should not exist")
        
        // Try to delete a non-existent key
        val deleteResult = storage.delete("non-existent-key")
        assertFalse(deleteResult, "Deleting non-existent key should return false")
        
        // Try to update metadata for a non-existent key
        val updateResult = storage.updateMetadata("non-existent-key", mapOf("key" to "value"))
        assertFalse(updateResult, "Updating metadata for non-existent key should return false")
    }
}