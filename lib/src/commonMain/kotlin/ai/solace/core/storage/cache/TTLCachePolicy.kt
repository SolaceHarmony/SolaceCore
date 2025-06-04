package ai.solace.core.storage.cache

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.time.Duration

/**
 * Implementation of the CachePolicy interface using the Time To Live (TTL) eviction strategy.
 *
 * This implementation evicts entries that have been in the cache for longer than a specified time period.
 * It maintains a map of entry creation times to determine when entries should be evicted.
 *
 * @param K The type of the key used to identify the cached data.
 * @param V The type of the value to be cached.
 * @param ttl The time to live for cache entries.
 * @param maxSize The maximum number of entries the cache can hold, or -1 for unlimited.
 */
class TTLCachePolicy<K, V>(
    private val ttl: Duration,
    private val maxSize: Int = -1
) : CachePolicy<K, V> {
    /**
     * The cache data structure.
     */
    private val cache = ConcurrentHashMap<K, V>()

    /**
     * Map of entry creation times.
     */
    private val creationTimes = ConcurrentHashMap<K, Long>()

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
            // If the cache is at capacity and this is a new entry, perform maintenance
            if (maxSize > 0 && cache.size >= maxSize && !cache.containsKey(key)) {
                maintenance()

                // If still at capacity after maintenance, evict the oldest entry
                if (cache.size >= maxSize) {
                    // Find the oldest entry
                    val oldestEntry = creationTimes.entries.minByOrNull { it.value }

                    // Remove the oldest entry
                    oldestEntry?.let {
                        cache.remove(it.key)
                        creationTimes.remove(it.key)
                    }
                }
            }

            // Add the new entry
            cache[key] = value
            creationTimes[key] = System.currentTimeMillis()
            true
        }
    }

    /**
     * Gets an entry from the cache.
     *
     * @param key The key to identify the value.
     * @return The cached value, or null if the key doesn't exist or the entry has expired.
     */
    override fun get(key: K): V? {
        return lock.read {
            val creationTime = creationTimes[key] ?: return@read null
            val currentTime = System.currentTimeMillis()

            // Check if the entry has expired
            if (currentTime - creationTime > ttl.inWholeMilliseconds) {
                // Remove expired entry
                lock.write {
                    cache.remove(key)
                    creationTimes.remove(key)
                }
                return@read null
            }

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
            creationTimes.remove(key)
            removed != null
        }
    }

    /**
     * Checks if a key exists in the cache and has not expired.
     *
     * @param key The key to check.
     * @return True if the key exists and the entry has not expired, false otherwise.
     */
    override fun contains(key: K): Boolean {
        return lock.read {
            val creationTime = creationTimes[key] ?: return@read false
            val currentTime = System.currentTimeMillis()

            // Check if the entry has expired
            if (currentTime - creationTime > ttl.inWholeMilliseconds) {
                // Remove expired entry
                lock.write {
                    cache.remove(key)
                    creationTimes.remove(key)
                }
                return@read false
            }

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
            creationTimes.clear()
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
     * @return The maximum number of entries the cache can hold, or -1 if there is no limit.
     */
    override fun maxSize(): Int {
        return maxSize
    }

    /**
     * Performs maintenance on the cache by removing expired entries.
     *
     * @return True if maintenance was performed successfully, false otherwise.
     */
    override fun maintenance(): Boolean {
        return lock.write {
            val currentTime = System.currentTimeMillis()
            val expiredKeys = creationTimes.entries
                .filter { currentTime - it.value > ttl.inWholeMilliseconds }
                .map { it.key }

            for (key in expiredKeys) {
                cache.remove(key)
                creationTimes.remove(key)
            }

            true
        }
    }
}
