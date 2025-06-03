package ai.solace.core.storage

/**
 * Generic interface for storage operations.
 *
 * This interface defines the basic operations for storing and retrieving data.
 * It is designed to be flexible enough to handle different types of data and storage backends.
 *
 * @param K The type of the key used to identify the data.
 * @param V The type of the value to be stored.
 */
interface Storage<K, V> {
    /**
     * Stores a value with the given key.
     *
     * @param key The key to identify the value.
     * @param value The value to store.
     * @param metadata Additional metadata for the value.
     * @return True if the value was stored successfully, false otherwise.
     */
    suspend fun store(key: K, value: V, metadata: Map<String, Any> = emptyMap()): Boolean

    /**
     * Retrieves a value with the given key.
     *
     * @param key The key to identify the value.
     * @return The value and its metadata, or null if the key doesn't exist.
     */
    suspend fun retrieve(key: K): Pair<V, Map<String, Any>>?

    /**
     * Lists all keys in the storage.
     *
     * @return A list of all keys.
     */
    suspend fun listKeys(): List<K>

    /**
     * Deletes a value with the given key.
     *
     * @param key The key to identify the value.
     * @return True if the value was deleted successfully, false otherwise.
     */
    suspend fun delete(key: K): Boolean

    /**
     * Checks if a key exists in the storage.
     *
     * @param key The key to check.
     * @return True if the key exists, false otherwise.
     */
    suspend fun exists(key: K): Boolean

    /**
     * Updates the metadata for a value with the given key.
     *
     * @param key The key to identify the value.
     * @param metadata The new metadata to set.
     * @return True if the metadata was updated successfully, false otherwise.
     */
    suspend fun updateMetadata(key: K, metadata: Map<String, Any>): Boolean
}