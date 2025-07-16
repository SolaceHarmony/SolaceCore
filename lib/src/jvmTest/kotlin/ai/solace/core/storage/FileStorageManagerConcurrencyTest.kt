package ai.solace.core.storage

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for thread safety and deadlock prevention in FileStorageManager.
 *
 * These tests verify that the FileStorageManager properly handles concurrent access
 * and prevents deadlocks in various scenarios.
 */
class FileStorageManagerConcurrencyTest {
    
    private lateinit var tempDir: String
    private lateinit var storageManager: FileStorageManager
    
    @BeforeTest
    fun setup() {
        // Create a temporary directory for testing
        tempDir = Files.createTempDirectory("file-storage-manager-concurrency-test").toString()
        
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
    
    /**
     * Tests concurrent access to the storage manager's methods.
     *
     * This test creates multiple coroutines that simultaneously register, get, and unregister
     * storage implementations to verify thread safety.
     */
    @Test
    fun testConcurrentAccess() {
        runBlocking {
            withTimeout(10.seconds) {
                val jobs = List(10) { index ->
                    launch {
                        val storage = FileStorage<String, String>(
                            baseDirectory = tempDir,
                            keySerializer = { it },
                            valueSerializer = { mapOf("value" to it) },
                            valueDeserializer = { it["value"] as String }
                        )
                        val storageName = "test$index"
                        
                        // Register storage
                        val registered = storageManager.registerStorage(
                            String::class.java,
                            String::class.java,
                            storage,
                            storageName
                        )
                        assertTrue(registered)
                        
                        // Small delay to increase chance of concurrent access
                        delay(10)
                        
                        // Get storage
                        val retrievedStorage = storageManager.getStorage(
                            String::class.java,
                            String::class.java,
                            storageName
                        )
                        assertNotNull(retrievedStorage)
                        
                        // Store and retrieve data
                        val key = "key-$index"
                        val value = "value-$index"
                        retrievedStorage.store(key, value)
                        val retrievedValue = retrievedStorage.retrieve(key)?.first
                        assertEquals(value, retrievedValue)
                        
                        // Small delay to increase chance of concurrent access
                        delay(10)
                        
                        // Unregister storage
                        val unregistered = storageManager.unregisterStorage(
                            String::class.java,
                            String::class.java,
                            storageName
                        )
                        assertTrue(unregistered)
                    }
                }
                
                jobs.forEach { it.join() }
            }
        }
    }
    
    /**
     * Tests that mutex locks are properly released after operations.
     *
     * This test verifies that operations that acquire locks don't cause deadlocks
     * by ensuring that subsequent operations can still acquire the locks.
     */
    @Test
    fun testMutexLockRelease() {
        runBlocking {
            withTimeout(5.seconds) {
                // Register a storage implementation (acquires a lock)
                val storage = FileStorage<String, String>(
                    baseDirectory = tempDir,
                    keySerializer = { it },
                    valueSerializer = { mapOf("value" to it) },
                    valueDeserializer = { it["value"] as String }
                )
                storageManager.registerStorage(
                    String::class.java,
                    String::class.java,
                    storage,
                    "test"
                )
                
                // Get the storage (acquires a lock)
                val retrievedStorage = storageManager.getStorage(
                    String::class.java,
                    String::class.java,
                    "test"
                )
                assertNotNull(retrievedStorage)
                
                // Unregister the storage (acquires a lock)
                val unregistered = storageManager.unregisterStorage(
                    String::class.java,
                    String::class.java,
                    "test"
                )
                assertTrue(unregistered)
                
                // If locks weren't properly released, this would deadlock
                storageManager.registerStorage(
                    String::class.java,
                    String::class.java,
                    storage,
                    "test2"
                )
            }
        }
    }
    
    /**
     * Tests that nested method calls don't cause deadlocks.
     *
     * This test verifies that methods that acquire locks don't cause deadlocks
     * when called in sequence or nested.
     */
    @Test
    fun testNestedMethodCalls() {
        runBlocking {
            withTimeout(5.seconds) {
                // Register multiple storage implementations
                for (i in 0 until 5) {
                    val storage = FileStorage<String, String>(
                        baseDirectory = tempDir,
                        keySerializer = { it },
                        valueSerializer = { mapOf("value" to it) },
                        valueDeserializer = { it["value"] as String }
                    )
                    storageManager.registerStorage(
                        String::class.java,
                        String::class.java,
                        storage,
                        "test$i"
                    )
                    
                    // Store some data
                    storage.store("key$i", "value$i")
                }
                
                // Clear all storage implementations (which internally accesses the storage map)
                // This would deadlock if the clearAll method didn't properly handle locks
                val result = storageManager.clearAll()
                assertTrue(result)
                
                // Verify that all data was cleared
                for (i in 0 until 5) {
                    val storage = storageManager.getStorage(
                        String::class.java,
                        String::class.java,
                        "test$i"
                    )
                    assertNotNull(storage)
                    assertNull(storage.retrieve("key$i"))
                }
            }
        }
    }
    
    /**
     * Tests high concurrency with many threads and operations.
     *
     * This test creates a large number of coroutines that perform various operations
     * on the storage manager to stress test its thread safety.
     */
    @Test
    fun testHighConcurrency() {
        runBlocking {
            withTimeout(30.seconds) {
                val jobs = List(20) { index ->
                    launch {
                        repeat(10) { i ->
                            val storage = FileStorage<String, String>(
                                baseDirectory = tempDir,
                                keySerializer = { it },
                                valueSerializer = { mapOf("value" to it) },
                                valueDeserializer = { it["value"] as String }
                            )
                            val storageName = "test$index-$i"
                            
                            // Register storage
                            storageManager.registerStorage(
                                String::class.java,
                                String::class.java,
                                storage,
                                storageName
                            )
                            
                            // Get storage
                            val retrievedStorage = storageManager.getStorage(
                                String::class.java,
                                String::class.java,
                                storageName
                            )
                            
                            if (retrievedStorage != null) {
                                // Store and retrieve data
                                val key = "key-$index-$i"
                                val value = "value-$index-$i"
                                retrievedStorage.store(key, value)
                                retrievedStorage.retrieve(key)
                            }
                            
                            // Unregister storage
                            storageManager.unregisterStorage(
                                String::class.java,
                                String::class.java,
                                storageName
                            )
                            
                            // Small delay to prevent CPU overload
                            delay(5)
                        }
                    }
                }
                
                jobs.forEach { it.join() }
            }
        }
    }
    
    /**
     * Tests that the storage manager properly handles exceptions.
     *
     * This test verifies that exceptions thrown during operations are properly caught
     * and don't leave locks in an inconsistent state.
     */
    @Test
    fun testExceptionHandling() {
        runBlocking {
            withTimeout(5.seconds) {
                // Create a storage implementation that throws exceptions
                val exceptionStorage = object : Storage<String, String> {
                    override suspend fun store(key: String, value: String, metadata: Map<String, Any>): Boolean {
                        throw RuntimeException("Test exception")
                    }
                    
                    override suspend fun retrieve(key: String): Pair<String, Map<String, Any>>? {
                        throw RuntimeException("Test exception")
                    }
                    
                    override suspend fun listKeys(): List<String> {
                        throw RuntimeException("Test exception")
                    }
                    
                    override suspend fun delete(key: String): Boolean {
                        throw RuntimeException("Test exception")
                    }
                    
                    override suspend fun exists(key: String): Boolean {
                        throw RuntimeException("Test exception")
                    }
                    
                    override suspend fun updateMetadata(key: String, metadata: Map<String, Any>): Boolean {
                        throw RuntimeException("Test exception")
                    }
                }
                
                // Register the exception-throwing storage
                storageManager.registerStorage(
                    String::class.java,
                    String::class.java,
                    exceptionStorage,
                    "exception"
                )
                
                // Clear all storage implementations
                // This should handle the exception and return false
                val result = storageManager.clearAll()
                
                // The operation should complete without throwing an exception
                // but might return false due to the exception
                
                // Verify that we can still perform operations after the exception
                val storage = FileStorage<String, String>(
                    baseDirectory = tempDir,
                    keySerializer = { it },
                    valueSerializer = { mapOf("value" to it) },
                    valueDeserializer = { it["value"] as String }
                )
                val registered = storageManager.registerStorage(
                    String::class.java,
                    String::class.java,
                    storage,
                    "test"
                )
                assertTrue(registered)
            }
        }
    }
    
    /**
     * Tests concurrent access to the isActive flag.
     *
     * This test verifies that the isActive flag is properly protected by a mutex
     * and can be safely accessed concurrently.
     */
    @Test
    fun testConcurrentLifecycleOperations() {
        runBlocking {
            withTimeout(10.seconds) {
                // First, ensure the storage manager is in a known state
                storageManager.stop()
                assertFalse(storageManager.isActive())
                
                // Test that multiple start operations don't cause issues
                val startJobs = List(5) {
                    launch {
                        repeat(10) {
                            storageManager.start()
                            delay(5)
                        }
                    }
                }
                startJobs.forEach { it.join() }
                
                // Verify that the storage manager is active after all start operations
                assertTrue(storageManager.isActive())
                
                // Test that multiple stop operations don't cause issues
                val stopJobs = List(5) {
                    launch {
                        repeat(10) {
                            storageManager.stop()
                            delay(5)
                        }
                    }
                }
                stopJobs.forEach { it.join() }
                
                // Verify that the storage manager is not active after all stop operations
                assertFalse(storageManager.isActive())
                
                // Test alternating start/stop operations
                storageManager.start()
                assertTrue(storageManager.isActive())
                
                storageManager.stop()
                assertFalse(storageManager.isActive())
                
                storageManager.start()
                assertTrue(storageManager.isActive())
            }
        }
    }
}