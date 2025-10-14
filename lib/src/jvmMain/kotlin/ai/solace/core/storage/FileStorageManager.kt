package ai.solace.core.storage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Paths

/**
 * File-based implementation of the StorageManager interface.
 *
 * This implementation manages file-based storage implementations for different types of data.
 * It provides a unified interface for accessing these storage implementations and managing the storage system as a whole.
 *
 * @param baseDirectory The base directory where data will be stored.
 */
class FileStorageManager(
    private val baseDirectory: String
) : StorageManager {
    /**
     * The configuration storage implementation.
     */
    private val configurationStorage = FileConfigurationStorage(baseDirectory)

    /**
     * The actor state storage implementation.
     */
    private val actorStateStorage = FileActorStateStorage(baseDirectory)

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

    init {
        // Create the base directory if it doesn't exist
        Files.createDirectories(Paths.get(baseDirectory))
    }

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
     * Note: This method acquires a mutex lock to ensure thread-safe access to the storage map.
     *
     * @param keyClass The class of the key type.
     * @param valueClass The class of the value type.
     * @param storageName The name of the storage implementation to get.
     * @return The storage implementation, or null if no implementation is available for the specified types.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <K : Any, V : Any> getStorage(keyClass: kotlin.reflect.KClass<K>, valueClass: kotlin.reflect.KClass<V>, storageName: String): Storage<K, V>? {
        val key = Triple(keyClass.qualifiedName ?: keyClass.toString(), valueClass.qualifiedName ?: valueClass.toString(), storageName)
        // Use runBlocking to make this synchronous method thread-safe
        return kotlinx.coroutines.runBlocking {
            mutex.withLock {
                storageMap[key] as? Storage<K, V>
            }
        }
    }

    /**
     * Registers a storage implementation for the specified key and value types.
     * 
     * Note: This method acquires a mutex lock to ensure thread-safe access to the storage map.
     *
     * @param keyClass The class of the key type.
     * @param valueClass The class of the value type.
     * @param storage The storage implementation to register.
     * @param storageName The name to register the storage implementation under.
     * @return True if the storage implementation was registered successfully, false otherwise.
     */
    override fun <K : Any, V : Any> registerStorage(keyClass: kotlin.reflect.KClass<K>, valueClass: kotlin.reflect.KClass<V>, storage: Storage<K, V>, storageName: String): Boolean {
        val key = Triple(keyClass.qualifiedName ?: keyClass.toString(), valueClass.qualifiedName ?: valueClass.toString(), storageName)
        // Use runBlocking to make this synchronous method thread-safe
        return kotlinx.coroutines.runBlocking {
            mutex.withLock {
                storageMap[key] = storage
                true
            }
        }
    }

    /**
     * Unregisters a storage implementation for the specified key and value types.
     * 
     * Note: This method acquires a mutex lock to ensure thread-safe access to the storage map.
     *
     * @param keyClass The class of the key type.
     * @param valueClass The class of the value type.
     * @param storageName The name of the storage implementation to unregister.
     * @return True if the storage implementation was unregistered successfully, false otherwise.
     */
    override fun <K : Any, V : Any> unregisterStorage(keyClass: kotlin.reflect.KClass<K>, valueClass: kotlin.reflect.KClass<V>, storageName: String): Boolean {
        val key = Triple(keyClass.qualifiedName ?: keyClass.toString(), valueClass.qualifiedName ?: valueClass.toString(), storageName)
        // Use runBlocking to make this synchronous method thread-safe
        return kotlinx.coroutines.runBlocking {
            mutex.withLock {
                storageMap.remove(key) != null
            }
        }
    }

    /**
     * Flushes all pending changes to persistent storage.
     *
     * @return True if all changes were flushed successfully, false otherwise.
     */
    override suspend fun flushAll(): Boolean {
        // File-based storage implementations write changes immediately, so there's nothing to flush
        return true
    }

    /**
     * Clears all data from all storage implementations.
     *
     * Note: This method follows best practices for deadlock prevention by:
     * 1. Collecting all storage implementations that need to be cleared outside the lock
     * 2. Only acquiring the lock for the minimum time necessary
     * 3. Not calling methods on other objects while holding the lock
     *
     * @return True if all data was cleared successfully, false otherwise.
     */
    override suspend fun clearAll(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // First, get a snapshot of all storage implementations outside the lock
                val storagesToClear = mutableListOf<FileStorage<*, *>>()

                // Get a snapshot of the storage map values outside the lock
                val storageMapValues = mutex.withLock {
                    storageMap.values.toList()
                }

                // Add all FileStorage instances from the storage map
                storageMapValues.forEach { storage ->
                    if (storage is FileStorage<*, *>) {
                        storagesToClear.add(storage)
                    }
                }

                // Add the dedicated storage instances
                storagesToClear.add(configurationStorage)
                storagesToClear.add(actorStateStorage)

                // Now clear all storage implementations outside the lock
                for (storage in storagesToClear) {
                    storage.clearCache()
                }

                // Delete all files in the storage directories
                val configStorageDir = Paths.get(baseDirectory, "storage")
                if (Files.exists(configStorageDir)) {
                    try {
                        Files.walk(configStorageDir)
                            .sorted(Comparator.reverseOrder())
                            .forEach { Files.deleteIfExists(it) }
                    } catch (e: Exception) {
                        println("Error deleting files in storage directory: ${e.message}")
                        throw e
                    }
                }

                true
            } catch (e: Exception) {
                // Log the error
                println("Error clearing storage: ${e.message}")
                false
            }
        }
    }

    /**
     * Starts the storage manager.
     *
     * Note: This method acquires a mutex lock to ensure thread-safe access to the isActive flag.
     */
    override suspend fun start() {
        mutex.withLock {
            isActive = true
        }
    }

    /**
     * Stops the storage manager.
     *
     * Note: This method acquires a mutex lock to ensure thread-safe access to the isActive flag.
     */
    override suspend fun stop() {
        mutex.withLock {
            isActive = false
        }
    }

    /**
     * Checks if the storage manager is active.
     *
     * Note: This method acquires a mutex lock to ensure thread-safe access to the isActive flag.
     *
     * @return True if the storage manager is active, false otherwise.
     */
    override fun isActive(): Boolean {
        return kotlinx.coroutines.runBlocking {
            mutex.withLock {
                isActive
            }
        }
    }

    /**
     * Disposes of the storage manager and releases all resources.
     *
     * Note: This method handles exceptions that might be thrown during disposal.
     */
    override suspend fun dispose() {
        try {
            stop()
            // No need to clear data when disposing, as it's stored on disk
        } catch (e: Exception) {
            // Log the error
            println("Error disposing storage manager: ${e.message}")
        }
    }
}
