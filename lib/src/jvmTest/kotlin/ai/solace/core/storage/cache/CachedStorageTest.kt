package ai.solace.core.storage.cache

import ai.solace.core.storage.InMemoryStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

/**
 * Tests for the CachedStorage class.
 */
class CachedStorageTest {
    private lateinit var underlyingStorage: InMemoryStorage<String, String>
    private lateinit var cachePolicy: LRUCachePolicy<String, Pair<String, Map<String, Any>>>
    private lateinit var cachedStorage: CachedStorage<String, String>

    @BeforeTest
    fun setup() {
        // Create the underlying storage
        underlyingStorage = InMemoryStorage()

        // Create the cache policy
        cachePolicy = LRUCachePolicy(10)

        // Create the cached storage
        cachedStorage = CachedStorage(underlyingStorage, cachePolicy)
    }

    @Test
    fun `test store and retrieve`() = runBlocking {
        // Store a value
        assertTrue(cachedStorage.store("key1", "value1"))

        // Retrieve the value
        val result = cachedStorage.retrieve("key1")
        assertNotNull(result)
        assertEquals("value1", result.first)

        // Check that the value is in the cache
        val cached = cachePolicy.get("key1")
        assertNotNull(cached)
        assertEquals("value1", cached.first)
    }

    @Test
    fun `test retrieve from cache`() = runBlocking {
        // Store a value
        assertTrue(cachedStorage.store("key1", "value1"))

        // Retrieve the value to ensure it's in the cache
        cachedStorage.retrieve("key1")

        // Clear the underlying storage
        underlyingStorage.clear()

        // Retrieve the value again, it should come from the cache
        val result = cachedStorage.retrieve("key1")
        assertNotNull(result)
        assertEquals("value1", result.first)
    }

    @Test
    fun `test delete`() = runBlocking {
        // Store a value
        assertTrue(cachedStorage.store("key1", "value1"))

        // Delete the value
        assertTrue(cachedStorage.delete("key1"))

        // Check that the value is not in the cache
        assertNull(cachePolicy.get("key1"))

        // Check that the value is not in the underlying storage
        assertNull(underlyingStorage.retrieve("key1"))
    }

    @Test
    fun `test exists`() = runBlocking {
        // Store a value
        assertTrue(cachedStorage.store("key1", "value1"))

        // Check if the key exists
        assertTrue(cachedStorage.exists("key1"))

        // Check if a non-existent key exists
        assertFalse(cachedStorage.exists("key2"))
    }

    @Test
    fun `test exists with cache hit`() = runBlocking {
        // Store a value
        assertTrue(cachedStorage.store("key1", "value1"))

        // Retrieve the value to ensure it's in the cache
        cachedStorage.retrieve("key1")

        // Clear the underlying storage
        underlyingStorage.clear()

        // Check if the key exists, it should be found in the cache
        assertTrue(cachedStorage.exists("key1"))
    }

    @Test
    fun `test updateMetadata`() = runBlocking {
        // Store a value with metadata
        assertTrue(cachedStorage.store("key1", "value1", mapOf("meta1" to "value1")))

        // Retrieve the value to ensure it's in the cache
        val result1 = cachedStorage.retrieve("key1")
        assertNotNull(result1)
        assertEquals("value1", result1.first)
        assertEquals("value1", result1.second["meta1"])

        // Update the metadata
        assertTrue(cachedStorage.updateMetadata("key1", mapOf("meta1" to "value2")))

        // Retrieve the value again
        val result2 = cachedStorage.retrieve("key1")
        assertNotNull(result2)
        assertEquals("value1", result2.first)
        assertEquals("value2", result2.second["meta1"])

        // Check that the metadata is updated in the cache
        val cached = cachePolicy.get("key1")
        assertNotNull(cached)
        assertEquals("value2", cached.second["meta1"])
    }

    @Test
    fun `test listKeys`() = runBlocking {
        // Store some values
        assertTrue(cachedStorage.store("key1", "value1"))
        assertTrue(cachedStorage.store("key2", "value2"))

        // List the keys
        val keys = cachedStorage.listKeys()
        assertEquals(2, keys.size)
        assertTrue(keys.contains("key1"))
        assertTrue(keys.contains("key2"))
    }

    @Test
    fun `test clearCache`(): Unit = runBlocking {
        // Store some values
        assertTrue(cachedStorage.store("key1", "value1"))
        assertTrue(cachedStorage.store("key2", "value2"))

        // Clear the cache
        assertTrue(cachedStorage.clearCache())

        // Check that the cache is empty
        assertEquals(0, cachedStorage.cacheSize())

        // Check that the values are still in the underlying storage
        assertNotNull(underlyingStorage.retrieve("key1"))
        assertNotNull(underlyingStorage.retrieve("key2"))
    }

    @Test
    fun `test maintenance`() = runBlocking {
        // Store some values
        assertTrue(cachedStorage.store("key1", "value1"))
        assertTrue(cachedStorage.store("key2", "value2"))

        // Perform maintenance
        assertTrue(cachedStorage.maintenance())

        // Check that the values are still in the cache
        assertEquals(2, cachedStorage.cacheSize())
    }

    @Test
    fun `test cacheSize and cacheMaxSize`() = runBlocking {
        // Check initial cache size
        assertEquals(0, cachedStorage.cacheSize())

        // Store some values
        assertTrue(cachedStorage.store("key1", "value1"))
        assertTrue(cachedStorage.store("key2", "value2"))

        // Check cache size after adding values
        assertEquals(2, cachedStorage.cacheSize())

        // Check cache maximum size
        assertEquals(10, cachedStorage.cacheMaxSize())
    }

    @Test
    fun `test with TTL cache policy`() = runBlocking {
        // Create a cached storage with TTL cache policy
        val ttlCachePolicy = TTLCachePolicy<String, Pair<String, Map<String, Any>>>(100.milliseconds)
        val ttlCachedStorage = CachedStorage(underlyingStorage, ttlCachePolicy)

        // Store a value
        assertTrue(ttlCachedStorage.store("key1", "value1"))

        // Retrieve the value immediately
        val result1 = ttlCachedStorage.retrieve("key1")
        assertNotNull(result1)
        assertEquals("value1", result1.first)

        // Wait for the cache entry to expire
        delay(200) // Wait longer than the TTL

        // Retrieve the value again, it should come from the underlying storage
        val result2 = ttlCachedStorage.retrieve("key1")
        assertNotNull(result2)
        assertEquals("value1", result2.first)
    }
}
