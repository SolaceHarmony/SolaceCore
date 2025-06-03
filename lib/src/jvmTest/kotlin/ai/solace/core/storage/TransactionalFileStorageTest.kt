package ai.solace.core.storage

import kotlinx.coroutines.runBlocking
import org.junit.After
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.*

/**
 * Tests for the TransactionalFileStorage class.
 */
class TransactionalFileStorageTest {
    private lateinit var storage: TransactionalFileStorage<String, String>
    private val testDir = "build/test-storage"

    @BeforeTest
    fun setup() {
        // Create test directory
        Files.createDirectories(Paths.get(testDir))
        
        // Initialize storage
        storage = TransactionalFileStorage(
            baseDirectory = testDir,
            keySerializer = { it },
            valueSerializer = { mapOf("value" to it) },
            valueDeserializer = { it["value"] as String }
        )
    }

    @After
    fun cleanup() {
        // Delete test directory and all its contents
        Paths.get(testDir).toFile().deleteRecursively()
    }

    @Test
    fun `test begin transaction`() = runBlocking {
        // Initially, no transaction should be active
        assertFalse(storage.isActive())

        // Begin a transaction
        assertTrue(storage.begin())

        // Now a transaction should be active
        assertTrue(storage.isActive())

        // Trying to begin another transaction should fail
        assertFalse(storage.begin())
    }

    @Test
    fun `test commit transaction`() = runBlocking {
        // Store a value outside a transaction
        storage.store("key1", "value1")

        // Begin a transaction
        storage.begin()

        // Store a value within the transaction
        storage.storeInTransaction("key2", "value2")

        // The value should be visible within the transaction
        assertEquals("value2", storage.retrieve("key2")?.first)

        // Commit the transaction
        assertTrue(storage.commit())

        // The transaction should no longer be active
        assertFalse(storage.isActive())

        // The value should still be visible after the transaction is committed
        assertEquals("value2", storage.retrieve("key2")?.first)
    }

    @Test
    fun `test rollback transaction`() = runBlocking {
        // Store a value outside a transaction
        storage.store("key1", "value1")

        // Begin a transaction
        storage.begin()

        // Store a value within the transaction
        storage.storeInTransaction("key2", "value2")

        // The value should be visible within the transaction
        assertEquals("value2", storage.retrieve("key2")?.first)

        // Rollback the transaction
        assertTrue(storage.rollback())

        // The transaction should no longer be active
        assertFalse(storage.isActive())

        // The value should not be visible after the transaction is rolled back
        assertNull(storage.retrieve("key2"))
    }

    @Test
    fun `test storeInTransaction`() = runBlocking {
        // Begin a transaction
        storage.begin()

        // Store a value within the transaction
        assertTrue(storage.storeInTransaction("key1", "value1"))

        // The value should be visible within the transaction
        assertEquals("value1", storage.retrieve("key1")?.first)

        // Rollback the transaction
        storage.rollback()

        // The value should not be visible after the transaction is rolled back
        assertNull(storage.retrieve("key1"))
    }

    @Test
    fun `test deleteInTransaction`() = runBlocking {
        // Store a value outside a transaction
        storage.store("key1", "value1")

        // Begin a transaction
        storage.begin()

        // Delete the value within the transaction
        assertTrue(storage.deleteInTransaction("key1"))

        // The value should not be visible within the transaction
        assertNull(storage.retrieve("key1"))

        // Rollback the transaction
        storage.rollback()

        // The value should be visible again after the transaction is rolled back
        assertEquals("value1", storage.retrieve("key1")?.first)
    }

    @Test
    fun `test updateMetadataInTransaction`() = runBlocking {
        // Store a value with metadata outside a transaction
        storage.store("key1", "value1", mapOf("meta1" to "value1"))

        // Begin a transaction
        storage.begin()

        // Update the metadata within the transaction
        assertTrue(storage.updateMetadataInTransaction("key1", mapOf("meta1" to "value2")))

        // The updated metadata should be visible within the transaction
        assertEquals("value2", storage.retrieve("key1")?.second?.get("meta1"))

        // Rollback the transaction
        storage.rollback()

        // The original metadata should be visible again after the transaction is rolled back
        assertEquals("value1", storage.retrieve("key1")?.second?.get("meta1"))
    }

    @Test
    fun `test complex transaction scenario`() = runBlocking {
        // Store some values outside a transaction
        storage.store("key1", "value1")
        storage.store("key2", "value2")
        storage.store("key3", "value3")

        // Begin a transaction
        storage.begin()

        // Perform various operations within the transaction
        storage.storeInTransaction("key1", "new-value1") // Update existing value
        storage.deleteInTransaction("key2") // Delete existing value
        storage.storeInTransaction("key4", "value4") // Add new value
        storage.updateMetadataInTransaction("key3", mapOf("meta1" to "value1")) // Update metadata

        // Verify the state within the transaction
        assertEquals("new-value1", storage.retrieve("key1")?.first)
        assertNull(storage.retrieve("key2"))
        assertEquals("value3", storage.retrieve("key3")?.first)
        assertEquals("value1", storage.retrieve("key3")?.second?.get("meta1"))
        assertEquals("value4", storage.retrieve("key4")?.first)

        // Commit the transaction
        assertTrue(storage.commit())

        // Verify the state after the transaction is committed
        assertEquals("new-value1", storage.retrieve("key1")?.first)
        assertNull(storage.retrieve("key2"))
        assertEquals("value3", storage.retrieve("key3")?.first)
        assertEquals("value1", storage.retrieve("key3")?.second?.get("meta1"))
        assertEquals("value4", storage.retrieve("key4")?.first)

        // Begin another transaction
        storage.begin()

        // Perform more operations
        storage.storeInTransaction("key1", "newer-value1")
        storage.storeInTransaction("key2", "value2-restored")

        // Verify the state within the transaction
        assertEquals("newer-value1", storage.retrieve("key1")?.first)
        assertEquals("value2-restored", storage.retrieve("key2")?.first)

        // Rollback the transaction
        assertTrue(storage.rollback())

        // Verify the state after the transaction is rolled back
        assertEquals("new-value1", storage.retrieve("key1")?.first)
        assertNull(storage.retrieve("key2"))
    }

    @Test
    fun `test listKeys with transaction`() = runBlocking {
        // Store some values outside a transaction
        storage.store("key1", "value1")
        storage.store("key2", "value2")

        // Begin a transaction
        storage.begin()

        // Add a new key and delete an existing one
        storage.storeInTransaction("key3", "value3")
        storage.deleteInTransaction("key2")

        // Verify the keys within the transaction
        val keys = storage.listKeys()
        assertEquals(2, keys.size)
        assertTrue(keys.contains("key1"))
        assertFalse(keys.contains("key2"))
        assertTrue(keys.contains("key3"))

        // Rollback the transaction
        storage.rollback()

        // Verify the keys after the transaction is rolled back
        val keysAfterRollback = storage.listKeys()
        assertEquals(2, keysAfterRollback.size)
        assertTrue(keysAfterRollback.contains("key1"))
        assertTrue(keysAfterRollback.contains("key2"))
        assertFalse(keysAfterRollback.contains("key3"))
    }

    @Test
    fun `test exists with transaction`() = runBlocking {
        // Store a value outside a transaction
        storage.store("key1", "value1")

        // Begin a transaction
        storage.begin()

        // Add a new key and delete an existing one
        storage.storeInTransaction("key2", "value2")
        storage.deleteInTransaction("key1")

        // Verify existence within the transaction
        assertFalse(storage.exists("key1"))
        assertTrue(storage.exists("key2"))

        // Rollback the transaction
        storage.rollback()

        // Verify existence after the transaction is rolled back
        assertTrue(storage.exists("key1"))
        assertFalse(storage.exists("key2"))
    }

    @Test
    fun `test persistence across storage instances`() = runBlocking {
        // Store a value and commit it
        storage.store("key1", "value1")
        
        // Begin a transaction
        storage.begin()
        
        // Store another value in the transaction
        storage.storeInTransaction("key2", "value2")
        
        // Commit the transaction
        storage.commit()
        
        // Create a new storage instance pointing to the same directory
        val newStorage = TransactionalFileStorage<String, String>(
            baseDirectory = testDir,
            keySerializer = { it },
            valueSerializer = { mapOf("value" to it) },
            valueDeserializer = { it["value"] as String }
        )
        
        // Verify that both values are visible in the new instance
        assertEquals("value1", newStorage.retrieve("key1")?.first)
        assertEquals("value2", newStorage.retrieve("key2")?.first)
    }
}