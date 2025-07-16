package ai.solace.core.storage.cache

import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * Tests for the TTLCachePolicy class.
 */
class TTLCachePolicyTest {
    private lateinit var cache: TTLCachePolicy<String, String>
    
    @BeforeTest
    fun setup() {
        // Create a cache with a TTL of 1 second and a maximum size of 3
        cache = TTLCachePolicy(1.seconds, 3)
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
    fun `test TTL expiration`() = runBlocking {
        // Add an entry
        assertTrue(cache.add("key1", "value1"))
        
        // Entry should be available immediately
        assertEquals("value1", cache.get("key1"))
        
        // Wait for the entry to expire
        delay(1100) // Wait slightly longer than the TTL
        
        // Entry should be expired
        assertNull(cache.get("key1"))
    }
    
    @Test
    fun `test max size`() {
        // Add entries to fill the cache
        assertTrue(cache.add("key1", "value1"))
        assertTrue(cache.add("key2", "value2"))
        assertTrue(cache.add("key3", "value3"))
        
        // Cache is now full
        assertEquals(3, cache.size())
        
        // Add a new entry, which should trigger maintenance
        assertTrue(cache.add("key4", "value4"))
        
        // Check that one of the entries was evicted (we don't know which one since TTL doesn't specify order)
        assertEquals(3, cache.size())
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
    fun `test contains with expired entry`() = runBlocking {
        // Add an entry
        assertTrue(cache.add("key1", "value1"))
        
        // Entry should exist immediately
        assertTrue(cache.contains("key1"))
        
        // Wait for the entry to expire
        delay(1100) // Wait slightly longer than the TTL
        
        // Entry should no longer exist
        assertFalse(cache.contains("key1"))
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
    fun `test maintenance`() = runBlocking {
        // Add some entries
        assertTrue(cache.add("key1", "value1"))
        assertTrue(cache.add("key2", "value2"))
        
        // Wait for the entries to expire
        delay(1100) // Wait slightly longer than the TTL
        
        // Perform maintenance
        assertTrue(cache.maintenance())
        
        // Check that the expired entries were removed
        assertEquals(0, cache.size())
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
    
    @Test
    fun `test updating existing entry resets TTL`() = runBlocking {
        // Add an entry
        assertTrue(cache.add("key1", "value1"))
        
        // Wait for some time, but not enough to expire
        delay(500) // Half the TTL
        
        // Update the entry
        assertTrue(cache.add("key1", "new-value1"))
        
        // Wait for some more time, which would expire the original entry
        delay(700) // Total delay is now 1200ms, which is more than the TTL
        
        // Entry should still be available because the TTL was reset
        assertEquals("new-value1", cache.get("key1"))
    }
    
    @Test
    fun `test unlimited size`() {
        // Create a cache with no size limit
        val unlimitedCache = TTLCachePolicy<String, String>(1.seconds, -1)
        
        // Add many entries
        for (i in 1..100) {
            assertTrue(unlimitedCache.add("key$i", "value$i"))
        }
        
        // Check that all entries are still there
        assertEquals(100, unlimitedCache.size())
        for (i in 1..100) {
            assertEquals("value$i", unlimitedCache.get("key$i"))
        }
    }
    
    @Test
    fun `test very short TTL`() = runBlocking {
        // Create a cache with a very short TTL
        val shortTTLCache = TTLCachePolicy<String, String>(10.milliseconds)
        
        // Add an entry
        assertTrue(shortTTLCache.add("key1", "value1"))
        
        // Entry should be available immediately
        assertEquals("value1", shortTTLCache.get("key1"))
        
        // Wait for the entry to expire
        delay(20) // Wait longer than the TTL
        
        // Entry should be expired
        assertNull(shortTTLCache.get("key1"))
    }
}