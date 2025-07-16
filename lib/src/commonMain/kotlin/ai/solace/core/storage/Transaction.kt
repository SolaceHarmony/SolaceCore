package ai.solace.core.storage

/**
 * Interface for transaction operations in the storage system.
 *
 * This interface defines the basic operations for managing transactions,
 * which allow multiple storage operations to be performed atomically.
 * If any operation within a transaction fails, all operations are rolled back,
 * ensuring data consistency.
 */
interface Transaction {
    /**
     * Begins a new transaction.
     *
     * @return True if the transaction was started successfully, false otherwise.
     */
    suspend fun begin(): Boolean

    /**
     * Commits the current transaction, making all changes permanent.
     *
     * @return True if the transaction was committed successfully, false otherwise.
     */
    suspend fun commit(): Boolean

    /**
     * Rolls back the current transaction, undoing all changes.
     *
     * @return True if the transaction was rolled back successfully, false otherwise.
     */
    suspend fun rollback(): Boolean

    /**
     * Checks if a transaction is currently active.
     *
     * @return True if a transaction is active, false otherwise.
     */
    suspend fun isActive(): Boolean
}