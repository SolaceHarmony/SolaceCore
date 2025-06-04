package ai.solace.core.storage.cache

import ai.solace.core.storage.Storage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A wrapper class that adds caching capabilities to any Storage implementation.
 *
 * This class implements the Storage interface and delegates to the underlying storage
 * while using a cache policy to improve performance. It caches both values and metadata
 * to reduce the number of expensive operations on the underlying storage.
 *
 * @param K The type of the key used to identify the data.
 * @param V The type of the value to be stored.
 * @param storage The underlying storage implementation.
 * @param cachePolicy The cache policy to use for caching.
 */
class CachedStorage<K, V>(
    private val storage: Storage<K, V>,
    private val cachePolicy: CachePolicy<K, Pair<V, Map<String, Any>>>
) : Storage<K, V> {
    /**
     * Mutex for thread-safe access to the cache.
     */
    private val mutex = Mutex()

    /**
     * Stores a value with the given key.
     *
     * @param key The key to identify the value.
     * @param value The value to store.
     * @param metadata Additional metadata for the value.
     * @return True if the value was stored successfully, false otherwise.
     */
    override suspend fun store(key: K, value: V, metadata: Map<String, Any>): Boolean {
        // Store in the underlying storage first
        val result = storage.store(key, value, metadata)
        
        // If successful, update the cache
        if (result) {
            mutex.withLock {
                cachePolicy.add(key, Pair(value, metadata))
            }
        }
        
        return result
    }

    /**
     * Retrieves a value with the given key.
     *
     * @param key The key to identify the value.
     * @return The value and its metadata, or null if the key doesn't exist.
     */
    override suspend fun retrieve(key: K): Pair<V, Map<String, Any>>? {
        // Check the cache first
        val cached = mutex.withLock {
            cachePolicy.get(key)
        }
        
        if (cached != null) {
            return cached
        }
        
        // If not in cache, get from underlying storage
        val result = storage.retrieve(key) ?: return null
        
        // Cache the result
        mutex.withLock {
            cachePolicy.add(key, result)
        }
        
        return result
    }

    /**
     * Lists all keys in the storage.
     *
     * @return A list of all keys.
     */
    override suspend fun listKeys(): List<K> {
        // This operation is not cached, as it's difficult to cache effectively
        return storage.listKeys()
    }

    /**
     * Deletes a value with the given key.
     *
     * @param key The key to identify the value.
     * @return True if the value was deleted successfully, false otherwise.
     */
    override suspend fun delete(key: K): Boolean {
        // Delete from the underlying storage first
        val result = storage.delete(key)
        
        // If successful, remove from the cache
        if (result) {
            mutex.withLock {
                cachePolicy.remove(key)
            }
        }
        
        return result
    }

    /**
     * Checks if a key exists in the storage.
     *
     * @param key The key to check.
     * @return True if the key exists, false otherwise.
     */
    override suspend fun exists(key: K): Boolean {
        // Check the cache first
        val existsInCache = mutex.withLock {
            cachePolicy.contains(key)
        }
        
        if (existsInCache) {
            return true
        }
        
        // If not in cache, check the underlying storage
        return storage.exists(key)
    }

    /**
     * Updates the metadata for a value with the given key.
     *
     * @param key The key to identify the value.
     * @param metadata The new metadata to set.
     * @return True if the metadata was updated successfully, false otherwise.
     */
    override suspend fun updateMetadata(key: K, metadata: Map<String, Any>): Boolean {
        // Update in the underlying storage first
        val result = storage.updateMetadata(key, metadata)
        
        // If successful, update the cache
        if (result) {
            mutex.withLock {
                val cached = cachePolicy.get(key)
                if (cached != null) {
                    cachePolicy.add(key, Pair(cached.first, metadata))
                }
            }
        }
        
        return result
    }

    /**
     * Clears the cache.
     *
     * @return True if the cache was cleared successfully, false otherwise.
     */
    suspend fun clearCache(): Boolean {
        return mutex.withLock {
            cachePolicy.clear()
        }
    }

    /**
     * Performs maintenance on the cache.
     *
     * @return True if maintenance was performed successfully, false otherwise.
     */
    suspend fun maintenance(): Boolean {
        return mutex.withLock {
            cachePolicy.maintenance()
        }
    }

    /**
     * Gets the current size of the cache.
     *
     * @return The number of entries in the cache.
     */
    suspend fun cacheSize(): Int {
        return mutex.withLock {
            cachePolicy.size()
        }
    }

    /**
     * Gets the maximum size of the cache.
     *
     * @return The maximum number of entries the cache can hold, or -1 if there is no limit.
     */
    suspend fun cacheMaxSize(): Int {
        return cachePolicy.maxSize()
    }
}