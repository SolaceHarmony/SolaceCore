package ai.solace.core.storage

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class FileStorageTest {

    private lateinit var tempDir: String
    private lateinit var storage: FileStorage<String, String>

    @BeforeTest
    fun setup() {
        // Create a temporary directory for testing
        tempDir = Files.createTempDirectory("file-storage-test").toString()
        
        // Create a FileStorage instance
        storage = FileStorage(
            baseDirectory = tempDir,
            keySerializer = { it },
            valueSerializer = { mapOf("value" to it) },
            valueDeserializer = { it["value"] as String }
        )
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
    fun testStoreAndRetrieve() = runBlocking {
        withTimeout(5.seconds) {
            // Store a value
            val key = "test-key"
            val value = "test-value"
            val metadata = mapOf("timestamp" to System.currentTimeMillis())
            
            val storeResult = storage.store(key, value, metadata)
            assertTrue(storeResult, "Store should succeed")
            
            // Retrieve the value
            val retrievedData = storage.retrieve(key)
            assertNotNull(retrievedData, "Retrieved data should not be null")
            
            val (retrievedValue, retrievedMetadata) = retrievedData
            assertEquals(value, retrievedValue, "Retrieved value should match stored value")
            assertEquals(metadata["timestamp"], retrievedMetadata["timestamp"], "Retrieved metadata should match stored metadata")
        }
    }

    @Test
    fun testListKeys() = runBlocking {
        withTimeout(5.seconds) {
            // Store multiple values
            val keys = listOf("key1", "key2", "key3")
            val value = "value"
            
            keys.forEach { key ->
                storage.store(key, value)
            }
            
            // List the keys
            val listedKeys = storage.listKeys()
            
            // Verify that all stored keys are listed
            assertEquals(keys.size, listedKeys.size, "Number of listed keys should match number of stored keys")
            keys.forEach { key ->
                assertTrue(listedKeys.contains(key), "Listed keys should contain $key")
            }
        }
    }

    @Test
    fun testDelete() = runBlocking {
        withTimeout(5.seconds) {
            // Store a value
            val key = "test-key"
            val value = "test-value"
            
            storage.store(key, value)
            
            // Verify that the value exists
            assertTrue(storage.exists(key), "Value should exist after storing")
            
            // Delete the value
            val deleteResult = storage.delete(key)
            assertTrue(deleteResult, "Delete should succeed")
            
            // Verify that the value no longer exists
            assertFalse(storage.exists(key), "Value should not exist after deleting")
            
            // Verify that retrieving the value returns null
            val retrievedData = storage.retrieve(key)
            assertNull(retrievedData, "Retrieved data should be null after deleting")
        }
    }

    @Test
    fun testExists() = runBlocking {
        withTimeout(5.seconds) {
            // Check if a non-existent key exists
            val key = "non-existent-key"
            assertFalse(storage.exists(key), "Non-existent key should not exist")
            
            // Store a value
            val value = "test-value"
            storage.store(key, value)
            
            // Check if the key exists
            assertTrue(storage.exists(key), "Key should exist after storing")
        }
    }

    @Test
    fun testUpdateMetadata() = runBlocking {
        withTimeout(5.seconds) {
            // Store a value with metadata
            val key = "test-key"
            val value = "test-value"
            val initialMetadata = mapOf("timestamp" to System.currentTimeMillis())
            
            storage.store(key, value, initialMetadata)
            
            // Update the metadata
            val updatedMetadata = mapOf(
                "timestamp" to System.currentTimeMillis(),
                "version" to 2
            )
            
            val updateResult = storage.updateMetadata(key, updatedMetadata)
            assertTrue(updateResult, "Update metadata should succeed")
            
            // Retrieve the value and verify the metadata
            val retrievedData = storage.retrieve(key)
            assertNotNull(retrievedData, "Retrieved data should not be null")
            
            val (retrievedValue, retrievedMetadata) = retrievedData
            assertEquals(value, retrievedValue, "Retrieved value should match stored value")
            assertEquals(updatedMetadata["timestamp"], retrievedMetadata["timestamp"], "Retrieved metadata should match updated metadata")
            assertEquals(updatedMetadata["version"], retrievedMetadata["version"], "Retrieved metadata should match updated metadata")
        }
    }

    @Test
    fun testClearCache() = runBlocking {
        withTimeout(5.seconds) {
            // Store a value
            val key = "test-key"
            val value = "test-value"
            
            storage.store(key, value)
            
            // Retrieve the value to cache it
            storage.retrieve(key)
            
            // Clear the cache
            storage.clearCache()
            
            // Verify that the value can still be retrieved
            val retrievedData = storage.retrieve(key)
            assertNotNull(retrievedData, "Retrieved data should not be null after clearing cache")
            assertEquals(value, retrievedData.first, "Retrieved value should match stored value after clearing cache")
        }
    }

    @Test
    fun testNonExistentKey() = runBlocking {
        withTimeout(5.seconds) {
            // Try to retrieve a non-existent key
            val key = "non-existent-key"
            val retrievedData = storage.retrieve(key)
            assertNull(retrievedData, "Retrieved data should be null for non-existent key")
            
            // Try to update metadata for a non-existent key
            val updateResult = storage.updateMetadata(key, mapOf("version" to 1))
            assertFalse(updateResult, "Update metadata should fail for non-existent key")
            
            // Try to delete a non-existent key
            val deleteResult = storage.delete(key)
            assertFalse(deleteResult, "Delete should fail for non-existent key")
        }
    }
}