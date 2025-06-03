package ai.solace.core.storage

/**
 * Interface for storage operations with transaction support.
 *
 * This interface extends both the Storage and Transaction interfaces,
 * providing a unified interface for storage operations that can be performed
 * within transactions. It allows for atomic operations across multiple storage
 * operations, ensuring data consistency.
 *
 * @param K The type of the key used to identify the data.
 * @param V The type of the value to be stored.
 */
interface TransactionalStorage<K, V> : Storage<K, V>, Transaction {
    /**
     * Stores a value with the given key within the current transaction.
     *
     * This method is similar to the store method in the Storage interface,
     * but it performs the operation within the current transaction.
     * If no transaction is active, it behaves like the regular store method.
     *
     * @param key The key to identify the value.
     * @param value The value to store.
     * @param metadata Additional metadata for the value.
     * @return True if the value was stored successfully, false otherwise.
     */
    suspend fun storeInTransaction(key: K, value: V, metadata: Map<String, Any> = emptyMap()): Boolean

    /**
     * Deletes a value with the given key within the current transaction.
     *
     * This method is similar to the delete method in the Storage interface,
     * but it performs the operation within the current transaction.
     * If no transaction is active, it behaves like the regular delete method.
     *
     * @param key The key to identify the value.
     * @return True if the value was deleted successfully, false otherwise.
     */
    suspend fun deleteInTransaction(key: K): Boolean

    /**
     * Updates the metadata for a value with the given key within the current transaction.
     *
     * This method is similar to the updateMetadata method in the Storage interface,
     * but it performs the operation within the current transaction.
     * If no transaction is active, it behaves like the regular updateMetadata method.
     *
     * @param key The key to identify the value.
     * @param metadata The new metadata to set.
     * @return True if the metadata was updated successfully, false otherwise.
     */
    suspend fun updateMetadataInTransaction(key: K, metadata: Map<String, Any>): Boolean
}