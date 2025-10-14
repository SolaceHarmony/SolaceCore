package ai.solace.core.storage.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Implementation of the CachePolicy interface using the Least Recently Used (LRU) eviction strategy.
 *
 * This implementation evicts the least recently used entries when the cache reaches its maximum size.
 * It uses a linked hash map to maintain the order of entries based on access time.
 *
 * @param K The type of the key used to identify the cached data.
 * @param V The type of the value to be cached.
 * @param maxSize The maximum number of entries the cache can hold.
 */
class LRUCachePolicy<K, V>(private val maxSize: Int) : CachePolicy<K, V> {
    /**
     * The cache data structure.
     */
    private val cache = mutableMapOf<K, V>()
    private val order = ArrayDeque<K>()

    /**
     * Lock for thread-safe access to the cache.
     */
    private val lock = Mutex()

    /**
     * Adds an entry to the cache.
     *
     * @param key The key to identify the value.
     * @param value The value to cache.
     * @return True if the entry was added successfully, false otherwise.
     */
    override fun add(key: K, value: V): Boolean = runBlockingWithLock {
        if (cache.containsKey(key)) {
            cache[key] = value
            // Move key to most-recent position
            order.remove(key)
            order.addLast(key)
            true
        } else {
            // Evict if needed
            if (maxSize > 0 && cache.size >= maxSize) {
                val evict = order.removeFirstOrNull()
                if (evict != null) cache.remove(evict)
            }
            cache[key] = value
            order.addLast(key)
            true
        }
    }

    /**
     * Gets an entry from the cache.
     *
     * @param key The key to identify the value.
     * @return The cached value, or null if the key doesn't exist.
     */
    override fun get(key: K): V? = runBlockingWithLock {
        val v = cache[key]
        if (v != null) {
            order.remove(key)
            order.addLast(key)
        }
        v
    }

    /**
     * Removes an entry from the cache.
     *
     * @param key The key to identify the value.
     * @return True if the entry was removed successfully, false otherwise.
     */
    override fun remove(key: K): Boolean = runBlockingWithLock {
        val existed = cache.remove(key) != null
        if (existed) order.remove(key)
        existed
    }

    /**
     * Checks if a key exists in the cache.
     *
     * @param key The key to check.
     * @return True if the key exists, false otherwise.
     */
    override fun contains(key: K): Boolean = runBlockingWithLock { cache.containsKey(key) }

    /**
     * Clears all entries from the cache.
     *
     * @return True if all entries were cleared successfully, false otherwise.
     */
    override fun clear(): Boolean = runBlockingWithLock {
        cache.clear()
        order.clear()
        true
    }

    /**
     * Gets the current size of the cache.
     *
     * @return The number of entries in the cache.
     */
    override fun size(): Int = runBlockingWithLock { cache.size }

    /**
     * Gets the maximum size of the cache.
     *
     * @return The maximum number of entries the cache can hold.
     */
    override fun maxSize(): Int = maxSize

    /**
     * Performs maintenance on the cache.
     * For LRU cache, this is a no-op as maintenance is performed automatically during add operations.
     *
     * @return True, as maintenance is always successful for LRU cache.
     */
    override fun maintenance(): Boolean = true

    // Helper to avoid adding kotlinx-coroutines in signatures of CachePolicy
    private fun <T> runBlockingWithLock(block: () -> T): T = kotlinx.coroutines.runBlocking { lock.withLock { block() } }
}
