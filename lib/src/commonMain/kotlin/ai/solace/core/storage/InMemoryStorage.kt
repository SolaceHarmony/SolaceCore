package ai.solace.core.storage

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory implementation of the Storage interface.
 *
 * This implementation stores data in memory using a mutable map.
 * It is useful for development and testing, but data is lost when the application is restarted.
 *
 * @param K The type of the key used to identify the data.
 * @param V The type of the value to be stored.
 */
open class InMemoryStorage<K, V> : Storage<K, V> {
    /**
     * Map of keys to values and their metadata.
     */
    protected val storage = mutableMapOf<K, Pair<V, MutableMap<String, Any>>>()

    /**
     * Mutex for thread-safe access to the storage map.
     */
    protected val mutex = Mutex()

    /**
     * Stores a value with the given key.
     *
     * @param key The key to identify the value.
     * @param value The value to store.
     * @param metadata Additional metadata for the value.
     * @return True if the value was stored successfully, false otherwise.
     */
    override suspend fun store(key: K, value: V, metadata: Map<String, Any>): Boolean {
        return mutex.withLock {
            storage[key] = Pair(value, metadata.toMutableMap())
            true
        }
    }

    /**
     * Retrieves a value with the given key.
     *
     * @param key The key to identify the value.
     * @return The value and its metadata, or null if the key doesn't exist.
     */
    override suspend fun retrieve(key: K): Pair<V, Map<String, Any>>? {
        return mutex.withLock {
            storage[key]
        }
    }

    /**
     * Lists all keys in the storage.
     *
     * @return A list of all keys.
     */
    override suspend fun listKeys(): List<K> {
        return mutex.withLock {
            storage.keys.toList()
        }
    }

    /**
     * Deletes a value with the given key.
     *
     * @param key The key to identify the value.
     * @return True if the value was deleted successfully, false otherwise.
     */
    override suspend fun delete(key: K): Boolean {
        return mutex.withLock {
            storage.remove(key) != null
        }
    }

    /**
     * Checks if a key exists in the storage.
     *
     * @param key The key to check.
     * @return True if the key exists, false otherwise.
     */
    override suspend fun exists(key: K): Boolean {
        return mutex.withLock {
            storage.containsKey(key)
        }
    }

    /**
     * Updates the metadata for a value with the given key.
     *
     * @param key The key to identify the value.
     * @param metadata The new metadata to set.
     * @return True if the metadata was updated successfully, false otherwise.
     */
    override suspend fun updateMetadata(key: K, metadata: Map<String, Any>): Boolean {
        return mutex.withLock {
            val entry = storage[key] ?: return@withLock false
            storage[key] = Pair(entry.first, metadata.toMutableMap())
            true
        }
    }

    /**
     * Clears all data from the storage.
     *
     * @return True if all data was cleared successfully, false otherwise.
     */
    suspend fun clear(): Boolean {
        return mutex.withLock {
            storage.clear()
            true
        }
    }
}
