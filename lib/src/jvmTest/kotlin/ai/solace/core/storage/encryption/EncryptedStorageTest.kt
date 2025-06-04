package ai.solace.core.storage.encryption

import ai.solace.core.storage.InMemoryStorage
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EncryptedStorageTest {
    private val encryptionStrategy = AESEncryptionStrategy()
    private val underlyingStorage = InMemoryStorage<String, ByteArray>()
    private val encryptedStorage = EncryptedStorage<String, String>(
        storage = underlyingStorage,
        encryptionStrategy = encryptionStrategy,
        valueSerializer = { it },
        valueDeserializer = { it }
    )

    @Test
    fun `test store and retrieve`() = runBlocking {
        // Store a value
        assertTrue(encryptedStorage.store("key1", "value1"))

        // The underlying storage should contain encrypted data
        val encryptedData = underlyingStorage.retrieve("key1")?.first
        assertNotNull(encryptedData)
        assertFalse(String(encryptedData).contains("value1"))

        // Retrieve the value
        val retrieved = encryptedStorage.retrieve("key1")
        assertNotNull(retrieved)
        assertEquals("value1", retrieved.first)
    }

    @Test
    fun `test store and retrieve with metadata`() = runBlocking {
        // Store a value with metadata
        val metadata = mapOf("created" to "2023-01-01", "version" to 1)
        assertTrue(encryptedStorage.store("key2", "value2", metadata))

        // Retrieve the value and metadata
        val retrieved = encryptedStorage.retrieve("key2")
        assertNotNull(retrieved)
        assertEquals("value2", retrieved.first)
        assertEquals("2023-01-01", retrieved.second["created"])
        assertEquals(1, retrieved.second["version"])
    }

    @Test
    fun `test update metadata`() = runBlocking {
        // Store a value with metadata
        val metadata = mapOf("created" to "2023-01-01", "version" to 1)
        assertTrue(encryptedStorage.store("key3", "value3", metadata))

        // Update the metadata
        val updatedMetadata = mapOf("created" to "2023-01-01", "version" to 2, "updated" to "2023-01-02")
        assertTrue(encryptedStorage.updateMetadata("key3", updatedMetadata))

        // Retrieve the value and updated metadata
        val retrieved = encryptedStorage.retrieve("key3")
        assertNotNull(retrieved)
        assertEquals("value3", retrieved.first)
        assertEquals("2023-01-01", retrieved.second["created"])
        assertEquals(2, retrieved.second["version"])
        assertEquals("2023-01-02", retrieved.second["updated"])
    }

    @Test
    fun `test delete`() = runBlocking {
        // Store a value
        assertTrue(encryptedStorage.store("key4", "value4"))

        // Delete the value
        assertTrue(encryptedStorage.delete("key4"))

        // The value should no longer exist
        assertFalse(encryptedStorage.exists("key4"))
    }

    @Test
    fun `test exists`() = runBlocking {
        // Store a value
        assertTrue(encryptedStorage.store("key5", "value5"))

        // Check if the key exists
        assertTrue(encryptedStorage.exists("key5"))

        // Check if a non-existent key exists
        assertFalse(encryptedStorage.exists("key6"))
    }

    @Test
    fun `test list keys`() = runBlocking {
        // Store some values
        assertTrue(encryptedStorage.store("key7", "value7"))
        assertTrue(encryptedStorage.store("key8", "value8"))

        // List all keys
        val keys = encryptedStorage.listKeys()
        assertTrue(keys.contains("key7"))
        assertTrue(keys.contains("key8"))
    }
}
