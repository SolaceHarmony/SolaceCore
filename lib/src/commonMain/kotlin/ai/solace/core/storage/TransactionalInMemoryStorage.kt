package ai.solace.core.storage

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory implementation of the TransactionalStorage interface.
 *
 * This implementation extends InMemoryStorage and adds transaction support.
 * It maintains a separate transaction storage that is used to store changes
 * made within a transaction. When a transaction is committed, the changes
 * are applied to the main storage. If a transaction is rolled back, the
 * transaction storage is discarded.
 *
 * @param K The type of the key used to identify the data.
 * @param V The type of the value to be stored.
 */
open class TransactionalInMemoryStorage<K, V> : InMemoryStorage<K, V>(), TransactionalStorage<K, V> {
    /**
     * Flag indicating whether a transaction is currently active.
     */
    private var transactionActive = false

    /**
     * Mutex for thread-safe access to the transaction state.
     */
    private val transactionMutex = Mutex()

    /**
     * Storage for changes made within a transaction.
     */
    private val transactionStorage = mutableMapOf<K, Pair<V, MutableMap<String, Any>>>()

    /**
     * Set of keys that have been deleted within a transaction.
     */
    private val transactionDeletes = mutableSetOf<K>()

    /**
     * Begins a new transaction.
     *
     * @return True if the transaction was started successfully, false otherwise.
     */
    override suspend fun begin(): Boolean {
        return transactionMutex.withLock {
            if (transactionActive) {
                // Transaction already active
                false
            } else {
                transactionActive = true
                transactionStorage.clear()
                transactionDeletes.clear()
                true
            }
        }
    }

    /**
     * Commits the current transaction, making all changes permanent.
     *
     * @return True if the transaction was committed successfully, false otherwise.
     */
    override suspend fun commit(): Boolean {
        return transactionMutex.withLock {
            if (!transactionActive) {
                // No transaction active
                false
            } else {
                // Apply all changes to the main storage
                mutex.withLock {
                    // Apply deletes
                    for (key in transactionDeletes) {
                        storage.remove(key)
                    }

                    // Apply stores and updates
                    for ((key, value) in transactionStorage) {
                        storage[key] = value
                    }
                }

                // Clear transaction state
                transactionActive = false
                transactionStorage.clear()
                transactionDeletes.clear()
                true
            }
        }
    }

    /**
     * Rolls back the current transaction, undoing all changes.
     *
     * @return True if the transaction was rolled back successfully, false otherwise.
     */
    override suspend fun rollback(): Boolean {
        return transactionMutex.withLock {
            if (!transactionActive) {
                // No transaction active
                false
            } else {
                // Clear transaction state
                transactionActive = false
                transactionStorage.clear()
                transactionDeletes.clear()
                true
            }
        }
    }

    /**
     * Checks if a transaction is currently active.
     *
     * @return True if a transaction is active, false otherwise.
     */
    override suspend fun isActive(): Boolean {
        return transactionMutex.withLock {
            transactionActive
        }
    }

    /**
     * Stores a value with the given key within the current transaction.
     *
     * @param key The key to identify the value.
     * @param value The value to store.
     * @param metadata Additional metadata for the value.
     * @return True if the value was stored successfully, false otherwise.
     */
    override suspend fun storeInTransaction(key: K, value: V, metadata: Map<String, Any>): Boolean {
        return transactionMutex.withLock {
            if (!transactionActive) {
                // No transaction active, use regular store
                store(key, value, metadata)
            } else {
                // Store in transaction storage
                transactionStorage[key] = Pair(value, metadata.toMutableMap())
                // Remove from deletes if it was previously deleted
                transactionDeletes.remove(key)
                true
            }
        }
    }

    /**
     * Deletes a value with the given key within the current transaction.
     *
     * @param key The key to identify the value.
     * @return True if the value was deleted successfully, false otherwise.
     */
    override suspend fun deleteInTransaction(key: K): Boolean {
        return transactionMutex.withLock {
            if (!transactionActive) {
                // No transaction active, use regular delete
                delete(key)
            } else {
                // Add to deletes
                transactionDeletes.add(key)
                // Remove from transaction storage if it was previously stored
                transactionStorage.remove(key)
                true
            }
        }
    }

    /**
     * Updates the metadata for a value with the given key within the current transaction.
     *
     * @param key The key to identify the value.
     * @param metadata The new metadata to set.
     * @return True if the metadata was updated successfully, false otherwise.
     */
    override suspend fun updateMetadataInTransaction(key: K, metadata: Map<String, Any>): Boolean {
        return transactionMutex.withLock {
            if (!transactionActive) {
                // No transaction active, use regular updateMetadata
                updateMetadata(key, metadata)
            } else {
                // Check if the key exists in transaction storage
                val entry = transactionStorage[key]
                if (entry != null) {
                    // Update metadata in transaction storage
                    transactionStorage[key] = Pair(entry.first, metadata.toMutableMap())
                    true
                } else {
                    // Check if the key exists in main storage and is not deleted
                    if (transactionDeletes.contains(key)) {
                        false
                    } else {
                        // Get from main storage
                        val mainEntry = mutex.withLock {
                            storage[key]
                        } ?: return@withLock false

                        // Store in transaction storage with updated metadata
                        transactionStorage[key] = Pair(mainEntry.first, metadata.toMutableMap())
                        true
                    }
                }
            }
        }
    }

    /**
     * Retrieves a value with the given key.
     *
     * This method is overridden to check the transaction storage first,
     * then fall back to the main storage if the key is not found in the
     * transaction storage and has not been deleted in the transaction.
     *
     * @param key The key to identify the value.
     * @return The value and its metadata, or null if the key doesn't exist.
     */
    override suspend fun retrieve(key: K): Pair<V, Map<String, Any>>? {
        return transactionMutex.withLock {
            if (!transactionActive) {
                // No transaction active, use regular retrieve
                super.retrieve(key)
            } else {
                // Check if the key has been deleted in the transaction
                if (transactionDeletes.contains(key)) {
                    null
                } else {
                    // Check if the key exists in transaction storage
                    transactionStorage[key] ?: super.retrieve(key)
                }
            }
        }
    }

    /**
     * Lists all keys in the storage.
     *
     * This method is overridden to include keys from both the transaction
     * storage and the main storage, excluding keys that have been deleted
     * in the transaction.
     *
     * @return A list of all keys.
     */
    override suspend fun listKeys(): List<K> {
        return transactionMutex.withLock {
            if (!transactionActive) {
                // No transaction active, use regular listKeys
                super.listKeys()
            } else {
                // Get keys from main storage
                val mainKeys = mutex.withLock {
                    storage.keys.toList()
                }

                // Combine keys from main storage and transaction storage,
                // excluding keys that have been deleted in the transaction
                (mainKeys + transactionStorage.keys)
                    .distinct()
                    .filter { !transactionDeletes.contains(it) }
            }
        }
    }

    /**
     * Checks if a key exists in the storage.
     *
     * This method is overridden to check the transaction storage first,
     * then fall back to the main storage if the key is not found in the
     * transaction storage and has not been deleted in the transaction.
     *
     * @param key The key to check.
     * @return True if the key exists, false otherwise.
     */
    override suspend fun exists(key: K): Boolean {
        return transactionMutex.withLock {
            if (!transactionActive) {
                // No transaction active, use regular exists
                super.exists(key)
            } else {
                // Check if the key has been deleted in the transaction
                if (transactionDeletes.contains(key)) {
                    false
                } else {
                    // Check if the key exists in transaction storage
                    transactionStorage.containsKey(key) || super.exists(key)
                }
            }
        }
    }
}