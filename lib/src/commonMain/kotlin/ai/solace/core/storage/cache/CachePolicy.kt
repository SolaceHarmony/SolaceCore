package ai.solace.core.storage.cache

/**
 * Interface for cache policies.
 *
 * This interface defines the contract for cache policies, which determine
 * how entries are added to, accessed from, and evicted from the cache.
 *
 * @param K The type of the key used to identify the cached data.
 * @param V The type of the value to be cached.
 */
interface CachePolicy<K, V> {
    /**
     * Adds an entry to the cache.
     *
     * @param key The key to identify the value.
     * @param value The value to cache.
     * @return True if the entry was added successfully, false otherwise.
     */
    fun add(key: K, value: V): Boolean

    /**
     * Gets an entry from the cache.
     *
     * @param key The key to identify the value.
     * @return The cached value, or null if the key doesn't exist or the entry has expired.
     */
    fun get(key: K): V?

    /**
     * Removes an entry from the cache.
     *
     * @param key The key to identify the value.
     * @return True if the entry was removed successfully, false otherwise.
     */
    fun remove(key: K): Boolean

    /**
     * Checks if a key exists in the cache.
     *
     * @param key The key to check.
     * @return True if the key exists and the entry has not expired, false otherwise.
     */
    fun contains(key: K): Boolean

    /**
     * Clears all entries from the cache.
     *
     * @return True if all entries were cleared successfully, false otherwise.
     */
    fun clear(): Boolean

    /**
     * Gets the current size of the cache.
     *
     * @return The number of entries in the cache.
     */
    fun size(): Int

    /**
     * Gets the maximum size of the cache.
     *
     * @return The maximum number of entries the cache can hold, or -1 if there is no limit.
     */
    fun maxSize(): Int

    /**
     * Performs maintenance on the cache, such as removing expired entries.
     *
     * @return True if maintenance was performed successfully, false otherwise.
     */
    fun maintenance(): Boolean
}