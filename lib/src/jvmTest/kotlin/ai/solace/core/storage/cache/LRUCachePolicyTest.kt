package ai.solace.core.storage.cache

import kotlin.test.*

/**
 * Tests for the LRUCachePolicy class.
 */
class LRUCachePolicyTest {
    private lateinit var cache: LRUCachePolicy<String, String>
    
    @BeforeTest
    fun setup() {
        // Create a cache with a maximum size of 3
        cache = LRUCachePolicy(3)
    }
    
    @Test
    fun `test add and get`() {
        // Add some entries
        assertTrue(cache.add("key1", "value1"))
        assertTrue(cache.add("key2", "value2"))
        
        // Get the entries
        assertEquals("value1", cache.get("key1"))
        assertEquals("value2", cache.get("key2"))
        
        // Get a non-existent entry
        assertNull(cache.get("key3"))
    }
    
    @Test
    fun `test LRU eviction`() {
        // Add entries to fill the cache
        assertTrue(cache.add("key1", "value1"))
        assertTrue(cache.add("key2", "value2"))
        assertTrue(cache.add("key3", "value3"))
        
        // Cache is now full
        assertEquals(3, cache.size())
        
        // Access key1 to make it the most recently used
        assertEquals("value1", cache.get("key1"))
        
        // Add a new entry, which should evict key2 (the least recently used)
        assertTrue(cache.add("key4", "value4"))
        
        // Check that key2 was evicted
        assertNull(cache.get("key2"))
        
        // Check that the other entries are still there
        assertEquals("value1", cache.get("key1"))
        assertEquals("value3", cache.get("key3"))
        assertEquals("value4", cache.get("key4"))
    }
    
    @Test
    fun `test remove`() {
        // Add some entries
        assertTrue(cache.add("key1", "value1"))
        assertTrue(cache.add("key2", "value2"))
        
        // Remove an entry
        assertTrue(cache.remove("key1"))
        
        // Check that the entry was removed
        assertNull(cache.get("key1"))
        
        // Check that the other entry is still there
        assertEquals("value2", cache.get("key2"))
        
        // Try to remove a non-existent entry
        assertFalse(cache.remove("key3"))
    }
    
    @Test
    fun `test contains`() {
        // Add some entries
        assertTrue(cache.add("key1", "value1"))
        
        // Check if entries exist
        assertTrue(cache.contains("key1"))
        assertFalse(cache.contains("key2"))
    }
    
    @Test
    fun `test clear`() {
        // Add some entries
        assertTrue(cache.add("key1", "value1"))
        assertTrue(cache.add("key2", "value2"))
        
        // Clear the cache
        assertTrue(cache.clear())
        
        // Check that the cache is empty
        assertEquals(0, cache.size())
        assertNull(cache.get("key1"))
        assertNull(cache.get("key2"))
    }
    
    @Test
    fun `test size and maxSize`() {
        // Check initial size
        assertEquals(0, cache.size())
        
        // Add some entries
        assertTrue(cache.add("key1", "value1"))
        assertTrue(cache.add("key2", "value2"))
        
        // Check size after adding entries
        assertEquals(2, cache.size())
        
        // Check maximum size
        assertEquals(3, cache.maxSize())
    }
    
    @Test
    fun `test maintenance`() {
        // Maintenance should always return true for LRU cache
        assertTrue(cache.maintenance())
    }
    
    @Test
    fun `test updating existing entry`() {
        // Add an entry
        assertTrue(cache.add("key1", "value1"))
        
        // Update the entry
        assertTrue(cache.add("key1", "new-value1"))
        
        // Check that the entry was updated
        assertEquals("new-value1", cache.get("key1"))
    }
}