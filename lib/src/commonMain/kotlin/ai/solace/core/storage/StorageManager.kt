package ai.solace.core.storage

import ai.solace.core.lifecycle.Lifecycle

/**
 * Interface for managing storage operations.
 *
 * This interface provides a unified interface for accessing different types of storage
 * and managing the storage system as a whole. It implements the Lifecycle interface
 * to ensure proper initialization and cleanup of storage resources.
 */
interface StorageManager : Lifecycle {
    /**
     * Gets the configuration storage.
     *
     * @return The configuration storage implementation.
     */
    fun getConfigurationStorage(): ConfigurationStorage

    /**
     * Gets the actor state storage.
     *
     * @return The actor state storage implementation.
     */
    fun getActorStateStorage(): ActorStateStorage

    /**
     * Gets a generic storage implementation for the specified key and value types.
     *
     * @param keyClass The class of the key type.
     * @param valueClass The class of the value type.
     * @param storageName The name of the storage implementation to get.
     * @return The storage implementation, or null if no implementation is available for the specified types.
     */
    fun <K, V> getStorage(keyClass: Class<K>, valueClass: Class<V>, storageName: String = "default"): Storage<K, V>?

    /**
     * Registers a storage implementation for the specified key and value types.
     *
     * @param keyClass The class of the key type.
     * @param valueClass The class of the value type.
     * @param storage The storage implementation to register.
     * @param storageName The name to register the storage implementation under.
     * @return True if the storage implementation was registered successfully, false otherwise.
     */
    fun <K, V> registerStorage(keyClass: Class<K>, valueClass: Class<V>, storage: Storage<K, V>, storageName: String = "default"): Boolean

    /**
     * Unregisters a storage implementation for the specified key and value types.
     *
     * @param keyClass The class of the key type.
     * @param valueClass The class of the value type.
     * @param storageName The name of the storage implementation to unregister.
     * @return True if the storage implementation was unregistered successfully, false otherwise.
     */
    fun <K, V> unregisterStorage(keyClass: Class<K>, valueClass: Class<V>, storageName: String = "default"): Boolean

    /**
     * Flushes all pending changes to persistent storage.
     *
     * @return True if all changes were flushed successfully, false otherwise.
     */
    suspend fun flushAll(): Boolean

    /**
     * Clears all data from all storage implementations.
     *
     * @return True if all data was cleared successfully, false otherwise.
     */
    suspend fun clearAll(): Boolean
}