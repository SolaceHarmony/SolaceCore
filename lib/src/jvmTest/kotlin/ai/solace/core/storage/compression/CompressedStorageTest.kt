package ai.solace.core.storage.compression

import ai.solace.core.storage.InMemoryStorage
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * Tests for the CompressedStorage class.
 */
class CompressedStorageTest {
    private lateinit var underlyingStorage: InMemoryStorage<String, ByteArray>
    private lateinit var compressedStorage: CompressedStorage<String, ByteArray>
    
    @BeforeTest
    fun setup() {
        // Create the underlying storage
        underlyingStorage = InMemoryStorage()
        
        // Create the compressed storage with a low threshold to ensure compression
        compressedStorage = CompressedStorage(
            storage = underlyingStorage,
            compressionThreshold = 10, // Compress anything larger than 10 bytes
            valueClass = ByteArray::class.java
        )
    }
    
    @Test
    fun `test store and retrieve small data (no compression)`(): Unit = runBlocking {
        // Create a small test string that won't be compressed
        val original = "Small"
        val originalBytes = original.toByteArray()
        
        // Store the data
        assertTrue(compressedStorage.store("key1", originalBytes))
        
        // Retrieve the data
        val result = compressedStorage.retrieve("key1")
        assertNotNull(result)
        
        // Verify that the data was not compressed
        val metadata = result.second
        assertFalse(metadata["compressed"] as Boolean)
        
        // Verify that the retrieved data matches the original
        assertContentEquals(originalBytes, result.first)
    }
    
    @Test
    fun `test store and retrieve large data (with compression)`(): Unit = runBlocking {
        // Create a large test string that will be compressed (highly repetitive for good compression ratio)
        val original = "AAAA".repeat(100) // 400 bytes of highly compressible data
        val originalBytes = original.toByteArray()
        
        // Store the data
        assertTrue(compressedStorage.store("key1", originalBytes))
        
        // Retrieve the data
        val result = compressedStorage.retrieve("key1")
        assertNotNull(result)
        
        // Verify that the data was compressed
        val metadata = result.second
        assertTrue(metadata["compressed"] as Boolean)
        
        // Verify that the retrieved data matches the original
        assertContentEquals(originalBytes, result.first)
    }
    
    @Test
    fun `test compression ratio`(): Unit = runBlocking {
        // Create a highly compressible string (repeated characters)
        val original = "A".repeat(1000)
        val originalBytes = original.toByteArray()
        
        // Store the data
        assertTrue(compressedStorage.store("key1", originalBytes))
        
        // Get the compression ratio
        val ratio = compressedStorage.getCompressionRatio("key1")
        
        // Verify that the compression ratio is significant (> 10x)
        assertTrue(ratio > 10.0)
    }
    
    @Test
    fun `test delete`(): Unit = runBlocking {
        // Store some data
        val original = "Test data"
        val originalBytes = original.toByteArray()
        assertTrue(compressedStorage.store("key1", originalBytes))
        
        // Verify that the data exists
        assertTrue(compressedStorage.exists("key1"))
        
        // Delete the data
        assertTrue(compressedStorage.delete("key1"))
        
        // Verify that the data no longer exists
        assertFalse(compressedStorage.exists("key1"))
        assertNull(compressedStorage.retrieve("key1"))
    }
    
    @Test
    fun `test update metadata`(): Unit = runBlocking {
        // Store some data with metadata
        val original = "Test data"
        val originalBytes = original.toByteArray()
        assertTrue(compressedStorage.store("key1", originalBytes, mapOf("meta1" to "value1")))
        
        // Retrieve the data and verify the metadata
        val result1 = compressedStorage.retrieve("key1")
        assertNotNull(result1)
        assertEquals("value1", result1.second["meta1"])
        
        // Update the metadata
        assertTrue(compressedStorage.updateMetadata("key1", mapOf("meta1" to "value2", "meta2" to "value3")))
        
        // Retrieve the data again and verify the updated metadata
        val result2 = compressedStorage.retrieve("key1")
        assertNotNull(result2)
        assertEquals("value2", result2.second["meta1"])
        assertEquals("value3", result2.second["meta2"])
        
        // Verify that the compression-related metadata is preserved
        assertEquals(result1.second["compressed"], result2.second["compressed"])
        if (result1.second.containsKey("originalSize")) {
            assertEquals(result1.second["originalSize"], result2.second["originalSize"])
        }
    }
    
    @Test
    fun `test list keys`(): Unit = runBlocking {
        // Store some data
        val data1 = "Data 1".toByteArray()
        val data2 = "Data 2".toByteArray()
        assertTrue(compressedStorage.store("key1", data1))
        assertTrue(compressedStorage.store("key2", data2))
        
        // List the keys
        val keys = compressedStorage.listKeys()
        
        // Verify that the keys are correct
        assertEquals(2, keys.size)
        assertTrue(keys.contains("key1"))
        assertTrue(keys.contains("key2"))
    }
}