package ai.solace.core.storage.cache

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

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
    private val cache = LinkedHashMap<K, V>(maxSize, 0.75f, true)

    /**
     * Lock for thread-safe access to the cache.
     */
    private val lock = ReentrantReadWriteLock()

    /**
     * Adds an entry to the cache.
     *
     * @param key The key to identify the value.
     * @param value The value to cache.
     * @return True if the entry was added successfully, false otherwise.
     */
    override fun add(key: K, value: V): Boolean {
        return lock.write {
            // If the cache is at capacity, remove the least recently used entry
            if (cache.size >= maxSize && !cache.containsKey(key)) {
                val lruKey = cache.keys.firstOrNull()
                lruKey?.let { cache.remove(it) }
            }

            // Add the new entry
            cache[key] = value
            true
        }
    }

    /**
     * Gets an entry from the cache.
     *
     * @param key The key to identify the value.
     * @return The cached value, or null if the key doesn't exist.
     */
    override fun get(key: K): V? {
        return lock.read {
            // Getting an entry updates its access time in the LinkedHashMap
            cache[key]
        }
    }

    /**
     * Removes an entry from the cache.
     *
     * @param key The key to identify the value.
     * @return True if the entry was removed successfully, false otherwise.
     */
    override fun remove(key: K): Boolean {
        return lock.write {
            val removed = cache.remove(key)
            removed != null
        }
    }

    /**
     * Checks if a key exists in the cache.
     *
     * @param key The key to check.
     * @return True if the key exists, false otherwise.
     */
    override fun contains(key: K): Boolean {
        return lock.read {
            cache.containsKey(key)
        }
    }

    /**
     * Clears all entries from the cache.
     *
     * @return True if all entries were cleared successfully, false otherwise.
     */
    override fun clear(): Boolean {
        return lock.write {
            cache.clear()
            true
        }
    }

    /**
     * Gets the current size of the cache.
     *
     * @return The number of entries in the cache.
     */
    override fun size(): Int {
        return lock.read {
            cache.size
        }
    }

    /**
     * Gets the maximum size of the cache.
     *
     * @return The maximum number of entries the cache can hold.
     */
    override fun maxSize(): Int {
        return maxSize
    }

    /**
     * Performs maintenance on the cache.
     * For LRU cache, this is a no-op as maintenance is performed automatically during add operations.
     *
     * @return True, as maintenance is always successful for LRU cache.
     */
    override fun maintenance(): Boolean {
        // No maintenance needed for LRU cache
        return true
    }
}
