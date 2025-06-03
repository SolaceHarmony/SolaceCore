package ai.solace.core.storage

import ai.solace.core.lifecycle.Lifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory implementation of the StorageManager interface.
 *
 * This implementation manages in-memory storage implementations for different types of data.
 * It is useful for development and testing, but data is lost when the application is restarted.
 */
class InMemoryStorageManager : StorageManager {
    /**
     * The configuration storage implementation.
     */
    private val configurationStorage = InMemoryConfigurationStorage()

    /**
     * The actor state storage implementation.
     */
    private val actorStateStorage = InMemoryActorStateStorage()

    /**
     * Map of storage implementations by key class, value class, and name.
     */
    private val storageMap = mutableMapOf<Triple<String, String, String>, Storage<*, *>>()

    /**
     * Mutex for thread-safe access to the storage map.
     */
    private val mutex = Mutex()

    /**
     * Coroutine scope for asynchronous operations.
     */
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Flag indicating whether the storage manager is active.
     */
    private var isActive = false

    /**
     * Gets the configuration storage.
     *
     * @return The configuration storage implementation.
     */
    override fun getConfigurationStorage(): ConfigurationStorage {
        return configurationStorage
    }

    /**
     * Gets the actor state storage.
     *
     * @return The actor state storage implementation.
     */
    override fun getActorStateStorage(): ActorStateStorage {
        return actorStateStorage
    }

    /**
     * Gets a generic storage implementation for the specified key and value types.
     *
     * @param keyClass The class of the key type.
     * @param valueClass The class of the value type.
     * @param storageName The name of the storage implementation to get.
     * @return The storage implementation, or null if no implementation is available for the specified types.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <K, V> getStorage(keyClass: Class<K>, valueClass: Class<V>, storageName: String): Storage<K, V>? {
        val key = Triple(keyClass.name, valueClass.name, storageName)
        return storageMap[key] as? Storage<K, V>
    }

    /**
     * Registers a storage implementation for the specified key and value types.
     *
     * @param keyClass The class of the key type.
     * @param valueClass The class of the value type.
     * @param storage The storage implementation to register.
     * @param storageName The name to register the storage implementation under.
     * @return True if the storage implementation was registered successfully, false otherwise.
     */
    override fun <K, V> registerStorage(keyClass: Class<K>, valueClass: Class<V>, storage: Storage<K, V>, storageName: String): Boolean {
        val key = Triple(keyClass.name, valueClass.name, storageName)
        storageMap[key] = storage
        return true
    }

    /**
     * Unregisters a storage implementation for the specified key and value types.
     *
     * @param keyClass The class of the key type.
     * @param valueClass The class of the value type.
     * @param storageName The name of the storage implementation to unregister.
     * @return True if the storage implementation was unregistered successfully, false otherwise.
     */
    override fun <K, V> unregisterStorage(keyClass: Class<K>, valueClass: Class<V>, storageName: String): Boolean {
        val key = Triple(keyClass.name, valueClass.name, storageName)
        return storageMap.remove(key) != null
    }

    /**
     * Flushes all pending changes to persistent storage.
     *
     * @return True if all changes were flushed successfully, false otherwise.
     */
    override suspend fun flushAll(): Boolean {
        // In-memory storage doesn't need to be flushed
        return true
    }

    /**
     * Clears all data from all storage implementations.
     *
     * @return True if all data was cleared successfully, false otherwise.
     */
    override suspend fun clearAll(): Boolean {
        return mutex.withLock {
            try {
                (configurationStorage as? InMemoryStorage<*, *>)?.clear()
                (actorStateStorage as? InMemoryStorage<*, *>)?.clear()
                
                storageMap.values.forEach { storage ->
                    if (storage is InMemoryStorage<*, *>) {
                        storage.clear()
                    }
                }
                
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Starts the storage manager.
     */
    override suspend fun start() {
        isActive = true
    }

    /**
     * Stops the storage manager.
     */
    override suspend fun stop() {
        isActive = false
    }

    /**
     * Checks if the storage manager is active.
     *
     * @return True if the storage manager is active, false otherwise.
     */
    override fun isActive(): Boolean {
        return isActive
    }

    /**
     * Disposes of the storage manager and releases all resources.
     */
    override suspend fun dispose() {
        stop()
        clearAll()
    }
}