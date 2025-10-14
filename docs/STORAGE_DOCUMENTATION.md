# SolaceCore Storage System Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Core Interfaces](#core-interfaces)
4. [Implementations](#implementations)
   - [In-Memory Implementations](#in-memory-implementations)
   - [File-Based Implementations](#file-based-implementations)
5. [Transaction Support](#transaction-support)
6. [Thread Safety and Deadlock Prevention](#thread-safety-and-deadlock-prevention)
7. [Testing](#testing)
8. [Current Status](#current-status)
9. [Future Plans](#future-plans)
10. [Usage Examples](#usage-examples)

## Overview

The SolaceCore Storage System provides a flexible and extensible way to store and retrieve data for the SolaceCore framework. It is designed to handle different types of data, including configuration data and actor state data, and to support different storage backends. The system is built with thread safety, performance, and reliability in mind, with special attention to preventing deadlocks in concurrent environments.

## Architecture

The storage system is built around a set of interfaces that define the contract for storage operations. These interfaces are implemented by different storage backends, allowing for flexibility in how data is stored and retrieved. The system also includes transaction support for atomic operations, ensuring data consistency even in the face of failures.

The architecture follows these key principles:
- **Separation of concerns**: Each component has a specific responsibility
- **Interface-based design**: Components interact through well-defined interfaces
- **Thread safety**: All operations are thread-safe
- **Extensibility**: New storage backends can be easily added
- **Reliability**: The system is designed to prevent deadlocks and handle errors gracefully

## Core Interfaces

### Storage<K, V>

The `Storage<K, V>` interface is the foundation of the storage system. It defines the basic operations for storing and retrieving data with keys of type `K` and values of type `V`.

```kotlin
interface Storage<K, V> {
    suspend fun store(key: K, value: V, metadata: Map<String, Any> = emptyMap()): Boolean
    suspend fun retrieve(key: K): Pair<V, Map<String, Any>>?
    suspend fun listKeys(): List<K>
    suspend fun delete(key: K): Boolean
    suspend fun exists(key: K): Boolean
    suspend fun updateMetadata(key: K, metadata: Map<String, Any>): Boolean
}
```

### ConfigurationStorage

The `ConfigurationStorage` interface extends `Storage<String, Map<String, Any>>` and provides specialized methods for storing and retrieving configuration data.

```kotlin
interface ConfigurationStorage : Storage<String, Map<String, Any>> {
    suspend fun getConfigValue(key: String, path: String): Any?
    suspend fun setConfigValue(key: String, path: String, value: Any): Boolean
    suspend fun getComponentConfig(componentId: String): Map<String, Any>?
    suspend fun setComponentConfig(componentId: String, config: Map<String, Any>): Boolean
    suspend fun getSystemConfig(): Map<String, Any>
    suspend fun setSystemConfig(config: Map<String, Any>): Boolean
}
```

### ActorStateStorage

The `ActorStateStorage` interface extends `Storage<String, Map<String, Any>>` and provides specialized methods for storing and retrieving actor state data.

```kotlin
interface ActorStateStorage : Storage<String, Map<String, Any>> {
    suspend fun getActorState(actorId: String): ActorState?
    suspend fun setActorState(actorId: String, state: ActorState): Boolean
    suspend fun getActorPorts(actorId: String): Map<String, Map<String, Any>>?
    suspend fun setActorPorts(actorId: String, ports: Map<String, Map<String, Any>>): Boolean
    suspend fun getActorMetrics(actorId: String): Map<String, Any>?
    suspend fun setActorMetrics(actorId: String, metrics: Map<String, Any>): Boolean
    suspend fun getActorCustomState(actorId: String): Map<String, Any>?
    suspend fun setActorCustomState(actorId: String, customState: Map<String, Any>): Boolean
}
```

### StorageManager

The `StorageManager` interface provides a unified interface for accessing different types of storage and managing the storage system as a whole. It implements the `Lifecycle` interface to ensure proper initialization and cleanup of storage resources. For multiplatform correctness, it uses `KClass` for type keys.

```kotlin
import kotlin.reflect.KClass

interface StorageManager : Lifecycle {
    fun getConfigurationStorage(): ConfigurationStorage
    fun getActorStateStorage(): ActorStateStorage
    fun <K : Any, V : Any> getStorage(
        keyClass: KClass<K>,
        valueClass: KClass<V>,
        storageName: String = "default"
    ): Storage<K, V>?
    fun <K : Any, V : Any> registerStorage(
        keyClass: KClass<K>,
        valueClass: KClass<V>,
        storage: Storage<K, V>,
        storageName: String = "default"
    ): Boolean
    fun <K : Any, V : Any> unregisterStorage(
        keyClass: KClass<K>,
        valueClass: KClass<V>,
        storageName: String = "default"
    ): Boolean
    suspend fun flushAll(): Boolean
    suspend fun clearAll(): Boolean
}
```

### Transaction

The `Transaction` interface defines the basic operations for managing transactions, which allow multiple storage operations to be performed atomically.

```kotlin
interface Transaction {
    suspend fun begin(): Boolean
    suspend fun commit(): Boolean
    suspend fun rollback(): Boolean
    suspend fun isActive(): Boolean
}
```

### TransactionalStorage<K, V>

The `TransactionalStorage<K, V>` interface extends both the `Storage<K, V>` and `Transaction` interfaces, providing a unified interface for storage operations that can be performed within transactions.

```kotlin
interface TransactionalStorage<K, V> : Storage<K, V>, Transaction {
    suspend fun storeInTransaction(key: K, value: V, metadata: Map<String, Any> = emptyMap()): Boolean
    suspend fun deleteInTransaction(key: K): Boolean
    suspend fun updateMetadataInTransaction(key: K, metadata: Map<String, Any>): Boolean
}
```

## Implementations

### In-Memory Implementations

The storage system includes in-memory implementations of all the interfaces for development and testing:

#### InMemoryStorage<K, V>

The `InMemoryStorage<K, V>` class implements the `Storage<K, V>` interface and stores data in memory using a mutable map. It is useful for development and testing, but data is lost when the application is restarted.

```kotlin
open class InMemoryStorage<K, V> : Storage<K, V> {
    protected val storage = mutableMapOf<K, Pair<V, MutableMap<String, Any>>>()
    protected val mutex = Mutex()

    // Implementation of Storage methods
}
```

#### InMemoryConfigurationStorage

The `InMemoryConfigurationStorage` class extends `InMemoryStorage<String, Map<String, Any>>` and implements the `ConfigurationStorage` interface. It provides specialized methods for storing and retrieving configuration data.

```kotlin
class InMemoryConfigurationStorage : InMemoryStorage<String, Map<String, Any>>(), ConfigurationStorage {
    // Implementation of ConfigurationStorage methods
}
```

#### InMemoryActorStateStorage

The `InMemoryActorStateStorage` class extends `InMemoryStorage<String, Map<String, Any>>` and implements the `ActorStateStorage` interface. It provides specialized methods for storing and retrieving actor state data.

```kotlin
class InMemoryActorStateStorage : InMemoryStorage<String, Map<String, Any>>(), ActorStateStorage {
    // Implementation of ActorStateStorage methods
}
```

#### InMemoryStorageManager

The `InMemoryStorageManager` class implements the `StorageManager` interface and provides a unified interface for accessing different types of in-memory storage.

```kotlin
class InMemoryStorageManager : StorageManager {
    private val configurationStorage = InMemoryConfigurationStorage()
    private val actorStateStorage = InMemoryActorStateStorage()
    private val storageMap = mutableMapOf<Pair<Class<*>, Class<*>>, MutableMap<String, Storage<*, *>>>()
    private val mutex = Mutex()
    private var isActive = false
    private val lifecycleMutex = Mutex()

    // Implementation of StorageManager methods
}
```

#### TransactionalInMemoryStorage<K, V>

The `TransactionalInMemoryStorage<K, V>` class extends `InMemoryStorage<K, V>` and implements the `TransactionalStorage<K, V>` interface. It provides transaction support for in-memory storage.

```kotlin
open class TransactionalInMemoryStorage<K, V> : InMemoryStorage<K, V>(), TransactionalStorage<K, V> {
    private var transactionActive = false
    private val transactionMutex = Mutex()
    private val transactionStorage = mutableMapOf<K, Pair<V, MutableMap<String, Any>>>()
    private val transactionDeletes = mutableSetOf<K>()

    // Implementation of Transaction methods
    // Implementation of TransactionalStorage methods
}
```

### File-Based Implementations

The storage system also includes file-based implementations of all the interfaces for persistent storage:

#### FileStorage<K, V>

The `FileStorage<K, V>` class implements the `Storage<K, V>` interface and stores data in files. It provides persistent storage that survives application restarts.

```kotlin
open class FileStorage<K, V>(
    private val baseDirectory: String,
    private val keySerializer: (K) -> String = { it.toString() },
    private val valueSerializer: (V) -> Map<String, Any> = { /* ... */ },
    private val valueDeserializer: (Map<String, Any>) -> V = { /* ... */ }
) : Storage<K, V> {
    private val storageDirectory: Path = Paths.get(baseDirectory, "storage")
    private val json = Json { prettyPrint = true }
    protected val mutex = Mutex()
    private val cache = ConcurrentHashMap<K, Pair<V, Map<String, Any>>>()

    // Implementation of Storage methods
}
```

#### FileConfigurationStorage

The `FileConfigurationStorage` class extends `FileStorage<String, Map<String, Any>>` and implements the `ConfigurationStorage` interface. It provides specialized methods for storing and retrieving configuration data in files.

```kotlin
class FileConfigurationStorage(
    baseDirectory: String
) : FileStorage<String, Map<String, Any>>(
    baseDirectory = baseDirectory,
    keySerializer = { it },
    valueSerializer = { it },
    valueDeserializer = { it }
), ConfigurationStorage {
    // Implementation of ConfigurationStorage methods
}
```

#### FileActorStateStorage

The `FileActorStateStorage` class extends `FileStorage<String, Map<String, Any>>` and implements the `ActorStateStorage` interface. It provides specialized methods for storing and retrieving actor state data in files.

```kotlin
class FileActorStateStorage(
    baseDirectory: String
) : FileStorage<String, Map<String, Any>>(
    baseDirectory = baseDirectory,
    keySerializer = { it },
    valueSerializer = { it },
    valueDeserializer = { it }
), ActorStateStorage {
    // Implementation of ActorStateStorage methods
}
```

#### FileStorageManager

The `FileStorageManager` class implements the `StorageManager` interface and provides a unified interface for accessing different types of file-based storage.

```kotlin
class FileStorageManager(
    private val baseDirectory: String
) : StorageManager {
    private val configurationStorage = FileConfigurationStorage(baseDirectory)
    private val actorStateStorage = FileActorStateStorage(baseDirectory)
    private val storageMap = mutableMapOf<Pair<Class<*>, Class<*>>, MutableMap<String, Storage<*, *>>>()
    private val mutex = Mutex()
    private var isActive = false
    private val lifecycleMutex = Mutex()

    // Implementation of StorageManager methods
}
```

#### TransactionalFileStorage<K, V>

The `TransactionalFileStorage<K, V>` class extends `FileStorage<K, V>` and implements the `TransactionalStorage<K, V>` interface. It provides transaction support for file-based storage.

```kotlin
open class TransactionalFileStorage<K, V>(
    baseDirectory: String,
    keySerializer: (K) -> String = { it.toString() },
    valueSerializer: (V) -> Map<String, Any> = { /* ... */ },
    valueDeserializer: (Map<String, Any>) -> V = { /* ... */ }
) : FileStorage<K, V>(
    baseDirectory = baseDirectory,
    keySerializer = keySerializer,
    valueSerializer = valueSerializer,
    valueDeserializer = valueDeserializer
), TransactionalStorage<K, V> {
    private var transactionActive = false
    private val transactionMutex = Mutex()
    private val transactionStorage = ConcurrentHashMap<K, Pair<V, Map<String, Any>>>()
    private val transactionDeletes = ConcurrentHashMap.newKeySet<K>()
    private val transactionDirectory: Path = Paths.get(baseDirectory, "transaction")

    // Implementation of Transaction methods
    // Implementation of TransactionalStorage methods
}
```

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

## Thread Safety and Deadlock Prevention

The storage system is designed to be thread-safe and to prevent deadlocks in concurrent environments. All operations are protected by mutex locks to ensure thread safety, and special attention has been paid to preventing deadlocks.

### Thread Safety

All storage implementations use mutex locks to protect access to shared resources. This ensures that operations are thread-safe and that data is not corrupted by concurrent access.

#### Use of runBlocking in Synchronous Methods

Some synchronous methods in the storage managers (`InMemoryStorageManager` and `FileStorageManager`) use `kotlinx.coroutines.runBlocking` to make these methods thread-safe by acquiring a mutex lock. This is necessary because these methods are part of interfaces that cannot be changed to use suspend functions without breaking backward compatibility.

The following methods use `runBlocking` for thread safety:
- `getStorage()`
- `registerStorage()`
- `unregisterStorage()`
- `isActive()`

**Justification:** While the project generally aims to avoid blocking calls in favor of coroutines, these specific uses of `runBlocking` are justified because:
1. They are used in synchronous interface methods that cannot be changed to suspend functions
2. They only block for a very short time to acquire a mutex lock and perform a simple operation
3. They are well-documented in the code with comments explaining their purpose
4. They follow best practices for deadlock prevention by minimizing the scope of the lock

**Note:** For new code, it's recommended to use suspend functions and coroutines instead of blocking calls whenever possible.

```kotlin
protected val mutex = Mutex()

suspend fun store(key: K, value: V, metadata: Map<String, Any>): Boolean {
    return mutex.withLock {
        storage[key] = Pair(value, metadata.toMutableMap())
        true
    }
}
```

### Deadlock Prevention

To prevent deadlocks, the storage system follows these best practices:

#### 1. Minimize Lock Scope

Only lock the mutex for the minimum time necessary to ensure thread safety. Perform as much work as possible outside the lock.

```kotlin
// Bad example (lock held for too long)
suspend fun setActorState(actorId: String, state: ActorState): Boolean {
    return mutex.withLock {
        val actorData = retrieve(actorId)?.first?.toMutableMap() ?: mutableMapOf()
        val metadata = retrieve(actorId)?.second?.toMutableMap() ?: mutableMapOf()

        // Make changes to actorData

        store(actorId, actorData, metadata)
    }
}

// Good example (minimize lock scope)
suspend fun setActorState(actorId: String, state: ActorState): Boolean {
    // Retrieve data outside the mutex lock
    val retrievedData = retrieve(actorId)
    val actorData = retrievedData?.first?.toMutableMap() ?: mutableMapOf()
    val metadata = retrievedData?.second?.toMutableMap() ?: mutableMapOf()

    // Create state data outside the lock
    val stateData = when (state) {
        is ActorState.Initialized -> mapOf("type" to "Initialized")
        is ActorState.Running -> mapOf("type" to "Running")
        is ActorState.Stopped -> mapOf("type" to "Stopped")
        is ActorState.Error -> mapOf("type" to "Error", "exception" to state.exception)
        is ActorState.Paused -> mapOf("type" to "Paused", "reason" to state.reason)
    }

    // Set state in actor data outside the lock
    actorData["state"] = stateData

    // Update the storage with mutex lock
    return mutex.withLock {
        storage[actorId] = Pair(actorData, metadata)
        true
    }
}
```

#### 2. Avoid Nested Locks

Never call a method that acquires the same lock from within a locked block. If you need to call such methods, do so outside the lock.

```kotlin
// Bad example (nested locks)
mutex.withLock {
    // This will cause a deadlock if retrieve() also acquires the same mutex
    val data = retrieve(key)
    // ...
}

// Good example (avoid nested locks)
// Call retrieve() outside the lock
val data = retrieve(key)

mutex.withLock {
    // Use the retrieved data inside the lock
    // ...
}
```

#### 3. Use Direct Access When Appropriate

In some cases, it may be appropriate to directly access the protected resource instead of calling methods that acquire locks. This should be done carefully and only when necessary.

```kotlin
// Instead of this (which may cause deadlocks):
mutex.withLock {
    val data = retrieve(key)
    // ...
    store(key, updatedData)
}

// Do this:
val data = retrieve(key)
// ...
mutex.withLock {
    storage[key] = updatedData
}
```

#### 4. Add Error Handling

Add error handling to prevent hanging if something goes wrong. Use try-catch blocks to catch exceptions and release locks properly.

```kotlin
return try {
    // Retrieve data outside the mutex lock
    val retrievedData = retrieve(key)
    // ...

    mutex.withLock {
        // Update storage
        true
    }
} catch (e: Exception) {
    // Log the error
    false
}
```

## Testing

The storage system includes comprehensive tests to ensure that it works correctly and handles edge cases properly. The tests are organized into the following categories:

### Unit Tests

Unit tests verify that individual components work correctly in isolation. They test the basic functionality of each storage implementation.

```kotlin
@Test
fun testStore() = runBlocking {
    val storage = InMemoryStorage<String, String>()
    assertTrue(storage.store("key", "value"))
    assertEquals("value", storage.retrieve("key")?.first)
}
```

### Thread Safety Tests

Thread safety tests verify that the storage system properly handles concurrent access and prevents deadlocks in various scenarios.

```kotlin
@Test
fun testConcurrentAccess() = runBlocking {
    val storage = InMemoryStorage<String, String>()
    val jobs = List(10) { index ->
        launch {
            storage.store("key$index", "value$index")
            assertEquals("value$index", storage.retrieve("key$index")?.first)
        }
    }
    jobs.forEach { it.join() }
}
```

### Transaction Tests

Transaction tests verify that the transaction support works correctly, ensuring that operations within a transaction are atomic and that data consistency is maintained.

```kotlin
@Test
fun testTransaction() = runBlocking {
    val storage = TransactionalInMemoryStorage<String, String>()
    storage.begin()
    storage.storeInTransaction("key1", "value1")
    storage.storeInTransaction("key2", "value2")
    storage.commit()
    assertEquals("value1", storage.retrieve("key1")?.first)
    assertEquals("value2", storage.retrieve("key2")?.first)
}
```

## Current Status

The storage system currently includes:

### Completed

- Core interfaces: `Storage<K, V>`, `ConfigurationStorage`, `ActorStateStorage`, `StorageManager`
- In-memory implementations: `InMemoryStorage<K, V>`, `InMemoryConfigurationStorage`, `InMemoryActorStateStorage`, `InMemoryStorageManager`
- File-based implementations: `FileStorage<K, V>`, `FileConfigurationStorage`, `FileActorStateStorage`, `FileStorageManager`
- Transaction support: `Transaction`, `TransactionalStorage<K, V>`, `TransactionalInMemoryStorage<K, V>`, `TransactionalFileStorage<K, V>`
- Comprehensive tests for all implementations
- Thread safety and deadlock prevention

### In Progress

- Caching support for improved performance
- Compression support for large data
- Encryption support for sensitive data

### Planned

- Database integration for scalability
- Distributed storage for clustering
- Integration with other components (actor system, workflow manager, scripting engine, monitoring system)
- Performance optimization for high throughput and low latency

## Future Plans

The future plans for the storage system include:

### Database Integration

Implementing database storage implementations for scalability:
- `DatabaseStorage<K, V>` for generic storage
- `DatabaseConfigurationStorage` for configuration data
- `DatabaseActorStateStorage` for actor state data
- `DatabaseStorageManager` for managing different types of storage

### Distributed Storage

Implementing distributed storage implementations for clustering:
- `DistributedStorage<K, V>` for generic storage
- `DistributedConfigurationStorage` for configuration data
- `DistributedActorStateStorage` for actor state data
- `DistributedStorageManager` for managing different types of storage

### Integration with Other Components

Integrating the storage system with other components of the SolaceCore framework:
- Integration with actor system
- Integration with workflow manager
- Integration with scripting engine
- Integration with monitoring system

### Performance Optimization

Optimizing the storage system for high throughput and low latency:
- Optimizing storage operations for high throughput
- Optimizing storage operations for low latency
- Implementing benchmarking tools for storage system
- Implementing performance monitoring for storage system

## Usage Examples

### Basic Usage

```kotlin
// Create a storage manager
val storageManager = InMemoryStorageManager()

// Start the storage manager
storageManager.start()

// Get the configuration storage
val configStorage = storageManager.getConfigurationStorage()

// Store a configuration value
configStorage.setConfigValue("database", "url", "jdbc:mysql://localhost:3306/mydb")

// Retrieve a configuration value
val url = configStorage.getConfigValue("database", "url")

// Get the actor state storage
val actorStateStorage = storageManager.getActorStateStorage()

// Store an actor state
actorStateStorage.setActorState("actor1", ActorState.Running)

// Retrieve an actor state
val state = actorStateStorage.getActorState("actor1")

// Stop the storage manager
storageManager.stop()

// Dispose of the storage manager
storageManager.dispose()
```

### Using Transactions

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

### Custom Storage Types

```kotlin
// Create a custom storage implementation
val customStorage = InMemoryStorage<UUID, MyData>()

// Register the custom storage with the storage manager
storageManager.registerStorage(UUID::class.java, MyData::class.java, customStorage)

// Get the custom storage from the storage manager
val retrievedStorage = storageManager.getStorage(UUID::class.java, MyData::class.java)

// Use the custom storage
val myData = MyData("example")
retrievedStorage.store(UUID.randomUUID(), myData)
```

### File-Based Storage

```kotlin
// Create a file-based storage manager
val storageManager = FileStorageManager("data")

// Start the storage manager
storageManager.start()

// Get the configuration storage
val configStorage = storageManager.getConfigurationStorage()

// Store a configuration value
configStorage.setConfigValue("database", "url", "jdbc:mysql://localhost:3306/mydb")

// Retrieve a configuration value
val url = configStorage.getConfigValue("database", "url")

// Stop the storage manager
storageManager.stop()

// Dispose of the storage manager
storageManager.dispose()
```
### JSON Serialization/Deserialization Rules

File-based storages and script metadata use kotlinx.serialization JSON with robust handling of nested structures:
- Nested Map/List values are serialized recursively to `JsonObject`/`JsonArray`.
- Number parsing prefers `Int` when possible, then `Long`, then `Double`.
- Booleans parse from case-insensitive "true"/"false"; other primitives remain strings.

### Compression and Encryption Wrappers

#### CompressedStorage<K, V>
Wraps a `Storage<K, V>` and compresses values larger than a configurable threshold. Metadata records:
- `compressed = true` (when threshold met)
- `originalSize = <bytes>` of the serialized (pre-compression) value

On retrieval, values are decompressed and deserialized back to `V`.

#### EncryptedStorage<K, V>
Wraps a `Storage<K, V>` and encrypts values at rest using an `EncryptionStrategy` (e.g., AES). On retrieval, values are decrypted before deserialization.

Note: Key management and rotation are deployment concerns and should be handled by the host application.
