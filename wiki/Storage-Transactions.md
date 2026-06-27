<!-- topic: Runtime -->
<!-- title: Storage Transactions -->

## Transaction Support

The storage system includes transaction support for atomic operations. Transactions allow multiple storage operations to be performed atomically, ensuring data consistency even in the face of failures. If any operation within a transaction fails, all operations are rolled back, ensuring data consistency.

### Transaction Interface

The `Transaction` interface defines the basic operations for managing transactions:

```kotlin
interface Transaction {
    suspend fun begin(): Boolean
    suspend fun commit(): Boolean
    suspend fun rollback(): Boolean
    suspend fun isActive(): Boolean
}
```

### TransactionalStorage Interface

The `TransactionalStorage<K, V>` interface extends both the `Storage<K, V>` and `Transaction` interfaces, providing a unified interface for storage operations that can be performed within transactions:

```kotlin
interface TransactionalStorage<K, V> : Storage<K, V>, Transaction {
    suspend fun storeInTransaction(key: K, value: V, metadata: Map<String, Any> = emptyMap()): Boolean
    suspend fun deleteInTransaction(key: K): Boolean
    suspend fun updateMetadataInTransaction(key: K, metadata: Map<String, Any>): Boolean
}
```

### Transaction Implementation

The transaction support is implemented in both in-memory and file-based storage:

#### TransactionalInMemoryStorage

The `TransactionalInMemoryStorage<K, V>` class extends `InMemoryStorage<K, V>` and implements the `TransactionalStorage<K, V>` interface. It maintains a separate transaction storage that is used to store changes made within a transaction. When a transaction is committed, the changes are applied to the main storage. If a transaction is rolled back, the transaction storage is discarded.

#### TransactionalFileStorage

The `TransactionalFileStorage<K, V>` class extends `FileStorage<K, V>` and implements the `TransactionalStorage<K, V>` interface. It maintains a separate transaction storage that is used to store changes made within a transaction. When a transaction is committed, the changes are applied to the main storage. If a transaction is rolled back, the transaction storage is discarded.

### Using Transactions

Here's an example of using transactions:

```kotlin
// Create a transactional storage
val storage = TransactionalInMemoryStorage<String, String>()

// Begin a transaction
storage.begin()

// Perform operations within the transaction
storage.storeInTransaction("key1", "value1")
storage.storeInTransaction("key2", "value2")

// Commit the transaction
storage.commit()

// Or rollback the transaction
// storage.rollback()
```


[Back to Storage & Persistence](Storage-and-Persistence)
