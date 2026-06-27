<!-- topic: Runtime -->
<!-- title: Storage & Persistence -->

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

---

# SolaceCore Storage System Checklist

## ✅ Completed

### Core Interfaces
- [x] Define `Storage<K, V>` interface for generic storage operations
- [x] Define `ConfigurationStorage` interface for configuration data
- [x] Define `ActorStateStorage` interface for actor state data
- [x] Define `StorageManager` interface for managing different types of storage
- [x] Define `StorageSerializer<T>` interface for serialization support

### In-Memory Implementations
- [x] Implement `InMemoryStorage<K, V>` for generic storage
- [x] Implement `InMemoryConfigurationStorage` for configuration data
- [x] Implement `InMemoryActorStateStorage` for actor state data
- [x] Implement `InMemoryStorageManager` for managing different types of storage
- [x] Implement `StorageSerializerRegistry` for serialization support

### Testing
- [x] Write tests for `InMemoryStorage<K, V>`
- [x] Write tests for `InMemoryConfigurationStorage`
- [x] Write tests for `InMemoryActorStateStorage`
- [x] Write tests for `InMemoryStorageManager`
- [x] Write tests for `StorageSerializerRegistry`

### Documentation
- [x] Document storage system architecture and design
- [x] Document storage interfaces and their usage
- [x] Document in-memory implementations
- [x] Document serialization support
- [x] Document deadlock prevention best practices

## ✅ Completed

### Core Interfaces
- [x] Define `Storage<K, V>` interface for generic storage operations
- [x] Define `ConfigurationStorage` interface for configuration data
- [x] Define `ActorStateStorage` interface for actor state data
- [x] Define `StorageManager` interface for managing different types of storage
- [x] Define `StorageSerializer<T>` interface for serialization support

### In-Memory Implementations
- [x] Implement `InMemoryStorage<K, V>` for generic storage
- [x] Implement `InMemoryConfigurationStorage` for configuration data
- [x] Implement `InMemoryActorStateStorage` for actor state data
- [x] Implement `InMemoryStorageManager` for managing different types of storage
- [x] Implement `StorageSerializerRegistry` for serialization support

### Persistent Storage
- [x] Design file-based storage implementations
- [x] Implement `FileStorage<K, V>` for generic storage
- [x] Implement `FileConfigurationStorage` for configuration data
- [x] Implement `FileActorStateStorage` for actor state data
- [x] Implement `FileStorageManager` for managing different types of storage

### Testing
- [x] Write tests for `InMemoryStorage<K, V>`
- [x] Write tests for `InMemoryConfigurationStorage`
- [x] Write tests for `InMemoryActorStateStorage`
- [x] Write tests for `InMemoryStorageManager`
- [x] Write tests for `StorageSerializerRegistry`
- [x] Write tests for `FileStorage<K, V>`
- [x] Write tests for `FileConfigurationStorage`
- [x] Write tests for `FileActorStateStorage`
- [x] Write tests for `FileStorageManager`

### Documentation
- [x] Document storage system architecture and design
- [x] Document storage interfaces and their usage
- [x] Document in-memory implementations
- [x] Document serialization support
- [x] Document deadlock prevention best practices

### Bug Fixes
- [x] Fix deadlocks in `InMemoryConfigurationStorage`
- [x] Fix deadlocks in `InMemoryActorStateStorage`
- [x] Add timeout handling to tests to prevent hanging

## ✅ Completed

### Advanced Features
- [x] Implement transaction support for atomic operations
  - [x] Define Transaction interface
  - [x] Create TransactionalStorage interface
  - [x] Implement TransactionalInMemoryStorage
  - [x] Implement TransactionalFileStorage
  - [x] Write tests for transaction support

## ✅ Completed

### Advanced Features
- [x] Implement caching support for improved performance
  - [x] Create CachePolicy interface
  - [x] Implement LRU (Least Recently Used) cache policy
  - [x] Implement TTL (Time To Live) cache policy
  - [x] Create CachedStorage wrapper class
  - [x] Write tests for caching support

## ✅ Completed

### Advanced Features
- [x] Implement compression support for large data
  - [x] Create CompressionStrategy interface
  - [x] Implement GZIPCompressionStrategy
  - [x] Create CompressedStorage wrapper class
  - [x] Write tests for compression support

## ✅ Completed

### Advanced Features
- [x] Implement encryption support for sensitive data
  - [x] Create EncryptionStrategy interface
  - [x] Implement AESEncryptionStrategy
  - [x] Create EncryptedStorage wrapper class
  - [x] Write tests for encryption support

## 📋 Todo

### Database Integration
- [ ] Design database storage implementations
- [ ] Implement `DatabaseStorage<K, V>` for generic storage
- [ ] Implement `DatabaseConfigurationStorage` for configuration data
- [ ] Implement `DatabaseActorStateStorage` for actor state data
- [ ] Implement `DatabaseStorageManager` for managing different types of storage

### Distributed Storage
- [ ] Design distributed storage implementations
- [ ] Implement `DistributedStorage<K, V>` for generic storage
- [ ] Implement `DistributedConfigurationStorage` for configuration data
- [ ] Implement `DistributedActorStateStorage` for actor state data
- [ ] Implement `DistributedStorageManager` for managing different types of storage

### Integration with Other Components
- [ ] Integrate storage system with actor system
- [ ] Integrate storage system with workflow manager
- [ ] Integrate storage system with scripting engine
- [ ] Integrate storage system with monitoring system

### Performance Optimization
- [ ] Optimize storage operations for high throughput
- [ ] Optimize storage operations for low latency
- [ ] Implement benchmarking tools for storage system
- [ ] Implement performance monitoring for storage system

## 📝 Notes

- The storage system is a critical component of the SolaceCore framework and should be designed for reliability, performance, and scalability.
- The in-memory implementations are useful for development and testing, but production systems should use persistent storage implementations.
- Deadlocks can occur when multiple threads attempt to acquire locks in different orders. Follow the best practices in the DEADLOCK_PREVENTION.md document to avoid deadlocks.
- The storage system should be designed to be extensible, allowing for different storage backends to be used as needed.
- The storage system should be designed to be thread-safe, allowing for concurrent access from multiple threads.

---

[← Architecture Overview](Architecture-Overview) · §3 of 15

---

## 3. Storage Module (`io.github.solaceharmony.core.storage`)
### 3.0. Strategic Storage Vision
Beyond the currently implemented storage solutions detailed below, the `Storage_Solutions_Design.md` document outlines a strategic vision for future data management capabilities within SolaceCore. This vision centers on a hybrid approach combining graph database technology with Kotlin-native storage solutions.

*   **Overarching Goal:** To provide robust and scalable data management by integrating Neo4j for complex data relationships and a Kotlin-native solution for structured data needs.

*   **Planned Neo4j Integration:**
    *   **Purpose:** To utilize Neo4j for graph-based storage, primarily for representing and querying relationships between actors, knowledge nodes, data flow, and other interconnected entities within the system.
    *   **Intended Use Cases:** Envisioned to support advanced scenarios such as Retrieval-Augmented Generation (RAG) by enabling intelligent and context-aware data retrieval based on these relationships.

*   **Planned Kotlin-Native Storage:**
    *   **Purpose:** To implement a Kotlin-native storage solution tailored for tabular or relational-style data.
    *   **Intended Use Cases:** To ensure actors and other components can persistently store intermediate results, structured states, and other non-graph data efficiently.

*   **Implementation Status (as per design document):**
    *   At the time the design document was authored, both the Neo4j integration and the Kotlin-native storage solution were in the planning stages and had not yet been implemented.

*   **Envisioned Future Enhancements for this Strategic Vision:**
    *   **Data Synchronization:** Mechanisms to synchronize data between the graph database (Neo4j) and the local Kotlin-native storage, ensuring consistency across different data models.
    *   **Advanced Querying:** Development of advanced querying capabilities that can leverage both storage types to support complex data retrieval and manipulation tasks.

This strategic direction suggests a future where SolaceCore can handle a diverse range of data types and relationships with specialized, high-performance storage backends, complementing the existing flexible storage abstractions.
The `storage` module provides a comprehensive framework for data persistence and management within SolaceCore. It defines core abstractions for storage operations, transaction management, serialization, and a centralized manager for accessing different storage implementations.

### 3.1. Core Storage Abstractions
The foundational interfaces of the storage module define how data is stored, retrieved, managed, and serialized.

#### 3.1.1. `Storage<K, V>` Interface
This generic interface is the primary contract for all storage implementations. It defines basic key-value storage operations.

*   **Purpose:** To provide a common API for interacting with various data storage backends.
*   **Generics:**
    *   `K`: The type of the key used to identify data.
    *   `V`: The type of the value to be stored.
*   **Key Methods:**
    *   `suspend fun store(key: K, value: V, metadata: Map<String, Any> = emptyMap()): Boolean`: Stores a value with its associated key and optional metadata.
    *   `suspend fun retrieve(key: K): Pair<V, Map<String, Any>>?`: Retrieves a value and its metadata by key. Returns `null` if the key is not found.
    *   `suspend fun listKeys(): List<K>`: Returns a list of all keys present in the storage.
    *   `suspend fun delete(key: K): Boolean`: Deletes a value associated with the given key.
    *   `suspend fun exists(key: K): Boolean`: Checks if a key exists in the storage.
    *   `suspend fun updateMetadata(key: K, metadata: Map<String, Any>): Boolean`: Updates the metadata for an existing key.

#### 3.1.2. `StorageManager` InterfaceThe `StorageManager` acts as a central coordinator and registry for various storage instances. It also manages the overall lifecycle of the storage system.

*   **Purpose:** To provide a unified access point to different types of storage and manage their lifecycles.
*   **Inheritance:** Implements `io.github.solaceharmony.core.lifecycle.Lifecycle` (and therefore `Disposable`).
*   **Key Methods:**
    *   `fun getConfigurationStorage(): ConfigurationStorage`: Retrieves a dedicated storage instance for configuration data.
    *   `fun getActorStateStorage(): ActorStateStorage`: Retrieves a dedicated storage instance for actor state.
    *   `fun <K, V> getStorage(keyClass: Class<K>, valueClass: Class<V>, storageName: String = "default"): Storage<K, V>?`: Retrieves a named generic storage instance for specified key/value types.
    *   `fun <K, V> registerStorage(keyClass: Class<K>, valueClass: Class<V>, storage: Storage<K, V>, storageName: String = "default"): Boolean`: Registers a storage implementation.
    *   `fun <K, V> unregisterStorage(keyClass: Class<K>, valueClass: Class<V>, storageName: String = "default"): Boolean`: Unregisters a storage implementation.
    *   `suspend fun flushAll(): Boolean`: Flushes all pending changes across all managed storages to their persistent backends.
    *   `suspend fun clearAll(): Boolean`: Clears all data from all managed storage implementations.
    *   Lifecycle methods (`start`, `stop`, `isActive`, `dispose`) are inherited for managing the storage system's state.

#### 3.1.3. `StorageSerializer<T>` Interface and `StorageSerializerRegistry`
These components handle the conversion of objects to and from a storable format.

*   **`StorageSerializer<T>` Interface:**
    *   **Purpose:** To define a contract for serializing objects of type `T` into a `Map<String, Any>` and deserializing them back.
    *   **Generic:** `T` - The type of object to be serialized/deserialized.
    *   **Key Methods:**
        *   `fun serialize(obj: T): Map<String, Any>`: Converts an object to a map.
        *   `fun deserialize(map: Map<String, Any>): T`: Converts a map back to an object.

*   **`StorageSerializerRegistry` Object:**
    *   **Purpose:** A global registry for `StorageSerializer` instances, allowing serializers to be registered for specific classes and retrieved when needed.
    *   **Key Methods:**
        *   `fun <T : Any> registerSerializer(clazz: Class<T>, serializer: StorageSerializer<T>)`: Registers a serializer for a given class.
        *   `fun <T : Any> getSerializer(clazz: Class<T>): StorageSerializer<T>?`: Retrieves a registered serializer for a class.
        *   `fun serialize(obj: Any): Map<String, Any>?`: Convenience method to serialize an object using its registered serializer.
        *   `fun <T : Any> deserialize(map: Map<String, Any>, clazz: Class<T>): T?`: Convenience method to deserialize a map to an object of a specific class using its registered serializer.

#### 3.1.4. `Transaction` Interface
Defines the contract for transactional operations, ensuring atomicity.

*   **Purpose:** To allow multiple storage operations to be grouped into a single atomic unit.
*   **Key Methods:**
    *   `suspend fun begin(): Boolean`: Starts a new transaction.
    *   `suspend fun commit(): Boolean`: Commits the current transaction, making changes permanent.
    *   `suspend fun rollback(): Boolean`: Rolls back the current transaction, discarding changes.
    *   `suspend fun isActive(): Boolean`: Checks if a transaction is currently active.

#### 3.1.5. `TransactionalStorage<K, V>` Interface
This interface combines basic storage operations with transactional capabilities.

*   **Purpose:** To provide an API for storage implementations that support atomic transactions.
*   **Inheritance:** Extends both `Storage<K, V>` and `Transaction`.
*   **Key Methods (in addition to those inherited):**
    *   `suspend fun storeInTransaction(key: K, value: V, metadata: Map<String, Any> = emptyMap()): Boolean`: Stores a value within the current transaction.
    *   `suspend fun deleteInTransaction(key: K): Boolean`: Deletes a value within the current transaction.
    *   `suspend fun updateMetadataInTransaction(key: K, metadata: Map<String, Any>): Boolean`: Updates metadata within the current transaction.

A conceptual diagram illustrating these core storage interfaces:

```mermaid
classDiagram
    direction LR

    package "io.github.solaceharmony.core.lifecycle" {
        interface Lifecycle {
            <<Interface>>
        }
    }

    package "io.github.solaceharmony.core.storage" {
        interface "Storage<K, V>" {
            <<Interface>>
            +store(key: K, value: V, metadata: Map): Boolean
            +retrieve(key: K): Pair<V, Map>?
            +listKeys(): List<K>
            +delete(key: K): Boolean
            +exists(key: K): Boolean
            +updateMetadata(key: K, metadata: Map): Boolean
        }

        interface StorageManager {
            <<Interface>>
            +getConfigurationStorage(): ConfigurationStorage
            +getActorStateStorage(): ActorStateStorage
            +getStorage(keyClass: Class, valueClass: Class, storageName: String): Storage?
            +registerStorage(keyClass: Class, valueClass: Class, storage: Storage, storageName: String): Boolean
            +unregisterStorage(keyClass: Class, valueClass: Class, storageName: String): Boolean
            +flushAll(): Boolean
            +clearAll(): Boolean
        }
        Lifecycle <|-- StorageManager

        interface "StorageSerializer<T>" {
            <<Interface>>
            +serialize(obj: T): Map
            +deserialize(map: Map): T
        }

        object StorageSerializerRegistry {
            +registerSerializer(clazz: Class, serializer: StorageSerializer)
            +getSerializer(clazz: Class): StorageSerializer?
            +serialize(obj: Any): Map?
            +deserialize(map: Map, clazz: Class): Any?
        }
        StorageSerializerRegistry ..> "StorageSerializer" : uses

        interface Transaction {
            <<Interface>>
            +begin(): Boolean
            +commit(): Boolean
            +rollback(): Boolean
            +isActive(): Boolean
        }

        interface "TransactionalStorage<K, V>" {
            <<Interface>>
            +storeInTransaction(key: K, value: V, metadata: Map): Boolean
            +deleteInTransaction(key: K): Boolean
            +updateMetadataInTransaction(key: K, metadata: Map): Boolean
        }
        "Storage<K, V>" <|-- "TransactionalStorage<K, V>"
        Transaction <|-- "TransactionalStorage<K, V>"

        StorageManager ..> "Storage" : manages/provides
        StorageManager ..> ConfigurationStorage
        StorageManager ..> ActorStateStorage
    }
    note for StorageManager "Actual ConfigurationStorage and ActorStateStorage interfaces are defined in their respective .kt files and will be detailed later."
```
These interfaces form a flexible and extensible foundation for managing various types of data persistence within SolaceCore, supporting different storage backends, serialization formats, and transactional semantics.
#### 3.1.2. Specialized Storage Interfaces
Building upon the generic `Storage<K, V>` interface, the module defines specialized contracts for common data types like actor state and configuration. Both of these specialized interfaces extend `Storage<String, Map<String, Any>>`, indicating they manage complex data structures (represented as maps) keyed by strings.

##### 3.1.2.1. `ActorStateStorage` Interface
This interface is dedicated to storing and retrieving all pertinent information related to an actor's state.

*   **Purpose:** To provide a tailored API for managing the persistence of actor states, including their core data, port configurations, metrics, and any custom state information.
*   **Inheritance:** Extends `Storage<String, Map<String, Any>>`. The `key` is typically the `actorId`.
*   **Key Specialized Methods:**
    *   `suspend fun getActorState(actorId: String): ActorState?`: Retrieves the primary state object for a given actor. (`ActorState` is defined in `io.github.solaceharmony.core.actor`).
    *   `suspend fun setActorState(actorId: String, state: ActorState): Boolean`: Sets the primary state object for an actor.
    *   `suspend fun getActorPorts(actorId: String): Map<String, Map<String, Any>>?`: Retrieves the port configurations for an actor.
    *   `suspend fun setActorPorts(actorId: String, ports: Map<String, Map<String, Any>>): Boolean`: Sets the port configurations for an actor.
    *   `suspend fun getActorMetrics(actorId: String): Map<String, Any>?`: Retrieves metrics associated with an actor.
    *   `suspend fun setActorMetrics(actorId: String, metrics: Map<String, Any>): Boolean`: Sets metrics for an actor.
    *   `suspend fun getActorCustomState(actorId: String): Map<String, Any>?`: Retrieves any additional custom state data for an actor.
    *   `suspend fun setActorCustomState(actorId: String, customState: Map<String, Any>): Boolean`: Sets custom state data for an actor.
    *   It also inherits the standard `store`, `retrieve`, `delete`, etc., methods from `Storage<String, Map<String, Any>>` which can be used to manage the actor's entire persisted data map directly if needed.

##### 3.1.2.2. `ConfigurationStorage` Interface
This interface is designed for managing configuration data for the overall system and its individual components.

*   **Purpose:** To provide a structured API for storing and retrieving configuration parameters, supporting hierarchical data access.
*   **Inheritance:** Extends `Storage<String, Map<String, Any>>`. The `key` can represent a component ID or a system-level configuration identifier.
*   **Key Specialized Methods:**
    *   `suspend fun getConfigValue(key: String, path: String): Any?`: Retrieves a specific configuration value from within a configuration map using a dot-separated `path` (e.g., "database.connection.url").
    *   `suspend fun setConfigValue(key: String, path: String, value: Any): Boolean`: Sets a specific configuration value within a configuration map using a dot-separated `path`.
    *   `suspend fun getComponentConfig(componentId: String): Map<String, Any>?`: Retrieves the entire configuration map for a specific component.
    *   `suspend fun setComponentConfig(componentId: String, config: Map<String, Any>): Boolean`: Sets the entire configuration map for a component.
    *   `suspend fun getSystemConfig(): Map<String, Any>`: Retrieves the system-wide configuration map.
    *   `suspend fun setSystemConfig(config: Map<String, Any>): Boolean`: Sets the system-wide configuration map.
    *   Inherited methods from `Storage<String, Map<String, Any>>` allow direct management of entire configuration maps.

These specialized interfaces are expected to be implemented by concrete storage backends (e.g., in-memory, file-based, database-backed) and made accessible via the `StorageManager`.
#### 3.1.3. In-Memory Storage Implementations
SolaceCore provides a set of concrete in-memory implementations for the storage interfaces, primarily useful for development, testing, or scenarios where persistence across application restarts is not required. These implementations reside in the `io.github.solaceharmony.core.storage` package.

##### 3.1.3.1. `InMemoryStorage<K, V>`
This open class serves as the base generic in-memory storage solution.

*   **Implements:** `Storage<K, V>`.
*   **Internal Structure:**
    *   Uses a `protected val storage = mutableMapOf<K, Pair<V, MutableMap<String, Any>>>()` to hold key-value pairs along with their metadata.
    *   Employs a `protected val mutex = Mutex()` from `kotlinx.coroutines.sync` to ensure thread-safe concurrent access to the internal `storage` map.
*   **Functionality:**
    *   Provides straightforward implementations for all methods defined in the `Storage<K, V>` interface (`store`, `retrieve`, `listKeys`, `delete`, `exists`, `updateMetadata`), performing operations on the internal map under the protection of the mutex.
    *   Includes an additional `suspend fun clear(): Boolean` method to remove all entries from this specific storage instance.

##### 3.1.3.2. `TransactionalInMemoryStorage<K, V>`
This open class extends `InMemoryStorage` to provide transactional capabilities.

*   **Implements:** `TransactionalStorage<K, V>` (and by extension, `Storage<K, V>` and `Transaction`).
*   **Inheritance:** Extends `InMemoryStorage<K, V>`.
*   **Transactional Logic:**
    *   Maintains a `private var transactionActive = false` flag.
    *   Uses a separate `private val transactionMutex = Mutex()` for managing transaction state.
    *   Keeps pending changes in:
        *   `private val transactionStorage = mutableMapOf<K, Pair<V, MutableMap<String, Any>>>()`: For new or updated entries.
        *   `private val transactionDeletes = mutableSetOf<K>()`: For keys marked for deletion.
    *   **`begin()`:** Sets `transactionActive` to true and clears `transactionStorage` and `transactionDeletes`.
    *   **`commit()`:** If a transaction is active, it acquires the parent `InMemoryStorage.mutex`, applies all deletes from `transactionDeletes` to the parent `storage`, then applies all stores/updates from `transactionStorage` to the parent `storage`. Finally, it resets the transaction state.
    *   **`rollback()`:** If a transaction is active, it simply clears `transactionStorage` and `transactionDeletes` and resets `transactionActive`.
    *   **`isActive()`:** Returns the state of `transactionActive` under `transactionMutex`.
    *   Transactional operations (`storeInTransaction`, `deleteInTransaction`, `updateMetadataInTransaction`) modify `transactionStorage` and `transactionDeletes` if a transaction is active; otherwise, they delegate to the parent `InMemoryStorage` methods.
    *   Read operations (`retrieve`, `listKeys`, `exists`) are overridden to first consult the `transactionStorage` and `transactionDeletes` if a transaction is active, before falling back to the parent `InMemoryStorage`'s data, thus providing a consistent view within a transaction.

##### 3.1.3.3. `InMemoryActorStateStorage`
Provides an in-memory implementation for storing actor states.

*   **Implements:** `ActorStateStorage`.
*   **Inheritance:** Extends `InMemoryStorage<String, Map<String, Any>>`.
*   **Functionality:**
    *   Leverages the base `InMemoryStorage` to store actor data, where each actor's entire state (including core state, ports, metrics, custom data) is a `Map<String, Any>` keyed by the `actorId`.
    *   The specialized methods (`getActorState`, `setActorState`, `getActorPorts`, etc.) interact with this map by accessing/modifying specific keys within it (e.g., "state", "ports", "metrics", "customState").
    *   The `setActorState` method handles the serialization of the `ActorState` sealed class instances (e.g., `ActorState.Running`, `ActorState.Error`) into a map structure (e.g., `mapOf("type" to "Running")`) before storing. `getActorState` performs the reverse deserialization.

##### 3.1.3.4. `InMemoryConfigurationStorage`
Provides an in-memory implementation for storing configuration data.

*   **Implements:** `ConfigurationStorage`.
*   **Inheritance:** Extends `InMemoryStorage<String, Map<String, Any>>`.
*   **Functionality:**
    *   Uses the base `InMemoryStorage` to store configuration maps.
    *   `getComponentConfig` and `setComponentConfig` use keys like `"component:<componentId>"`.
    *   `getSystemConfig` and `setSystemConfig` use the key `"system"`.
    *   The path-based methods (`getConfigValue`, `setConfigValue`) include logic to parse dot-separated paths (e.g., `"database.connection.url"`) and navigate the nested map structures representing the configuration. Helper methods `getValueFromPath`, `setValueAtPath`, and `isValidPath` facilitate this.

##### 3.1.3.5. `InMemoryStorageManager`
The in-memory implementation of the central storage coordinator.

*   **Implements:** `StorageManager` (and therefore `Lifecycle` and `Disposable`).
*   **Internal Structure:**
    *   Directly instantiates `InMemoryConfigurationStorage` and `InMemoryActorStateStorage` for the dedicated `getConfigurationStorage()` and `getActorStateStorage()` methods.
    *   Maintains a `private val storageMap = mutableMapOf<Triple<String, String, String>, Storage<*, *>>()` to hold other registered generic `Storage` instances. The key for this map is a `Triple` of (key class name, value class name, storage name).
    *   Uses a `Mutex` for thread-safe access to `storageMap` and its `isActive` lifecycle flag.
*   **Functionality:**
    *   `getStorage`, `registerStorage`, `unregisterStorage` operate on the `storageMap`.
    *   `flushAll()` is a no-op for in-memory implementations.
    *   `clearAll()` iterates through all managed `InMemoryStorage` instances (including the dedicated configuration and actor state storages, and those in `storageMap` if they are `InMemoryStorage` instances) and calls their respective `clear()` methods.
    *   Implements `start()`, `stop()`, `isActive()`, and `dispose()` (which calls `stop()` and `clearAll()`) for lifecycle management.

These in-memory classes provide a fully functional, albeit volatile, persistence layer for SolaceCore, crucial for ease of development and testing.
#### 3.1.4. Storage Caching Subsystem (`io.github.solaceharmony.core.storage.cache`)
To enhance performance, the `storage` module includes a caching subsystem that can wrap existing `Storage` implementations. This subsystem is located in the `io.github.solaceharmony.core.storage.cache` package and comprises a generic caching storage decorator and various cache eviction policies.

##### 3.1.4.1. `CachePolicy<K, V>` Interface
This interface defines the standard contract for all cache management and eviction strategies.

*   **Purpose:** To allow different caching behaviors (like LRU, TTL) to be plugged into the `CachedStorage`.
*   **Generics:**
    *   `K`: The type of the key.
    *   `V`: The type of the value being cached.
*   **Key Methods:**
    *   `fun add(key: K, value: V): Boolean`: Adds or updates an entry in the cache.
    *   `fun get(key: K): V?`: Retrieves an entry from the cache; may return `null` if not found or expired.
    *   `fun remove(key: K): Boolean`: Removes an entry from the cache.
    *   `fun contains(key: K): Boolean`: Checks if an unexpired entry for the key exists.
    *   `fun clear(): Boolean`: Clears all entries from the cache.
    *   `fun size(): Int`: Returns the current number of entries in the cache.
    *   `fun maxSize(): Int`: Returns the maximum capacity of the cache (-1 for unlimited).
    *   `fun maintenance(): Boolean`: Performs periodic maintenance, like evicting expired entries.

##### 3.1.4.2. Concrete Cache Policies
SolaceCore provides two concrete implementations of `CachePolicy`:

###### 3.1.4.2.A. `LRUCachePolicy<K, V>`
Implements a Least Recently Used (LRU) eviction strategy.
*   **Constructor:** `LRUCachePolicy<K, V>(private val maxSize: Int)`
*   **Mechanism:** Uses a `LinkedHashMap` configured for access-order. When the cache exceeds `maxSize`, the least recently accessed item is removed upon adding a new item.
*   **Thread Safety:** Uses `java.util.concurrent.locks.ReentrantReadWriteLock`.
*   **Maintenance:** The `maintenance()` method is a no-op as eviction is handled during `add`.

###### 3.1.4.2.B. `TTLCachePolicy<K, V>`
Implements a Time-To-Live (TTL) eviction strategy.
*   **Constructor:** `TTLCachePolicy<K, V>(private val ttl: Duration, private val maxSize: Int = -1)`
*   **Mechanism:** Stores entries along with their creation timestamps in `ConcurrentHashMap`s. Entries are considered expired if `currentTime - creationTime > ttl`.
    *   Expired entries are removed during `get()`, `contains()`, or explicitly via `maintenance()`.
    *   If `maxSize` is enforced and the cache is full, `add()` first runs `maintenance()`. If still full, it evicts the oldest (earliest creation time) entry.
*   **Thread Safety:** Uses `java.util.concurrent.locks.ReentrantReadWriteLock`.

##### 3.1.4.3. `CachedStorage<K, V>` Class
This class acts as a decorator, adding caching functionality to an existing `Storage` implementation.

*   **Implements:** `Storage<K, V>`.
*   **Constructor:** `CachedStorage<K, V>(private val storage: Storage<K, V>, private val cachePolicy: CachePolicy<K, Pair<V, Map<String, Any>>>)`
    *   It wraps an underlying `storage` instance.
    *   It uses a `cachePolicy` that stores `Pair<V, Map<String, Any>>`, meaning both the value and its associated metadata are cached together.
*   **Thread Safety:** Uses a `kotlinx.coroutines.sync.Mutex` to protect access to the `cachePolicy`.
*   **Operational Behavior:**
    *   **`store()`:** Writes to the underlying `storage` first. On success, it updates the `cachePolicy` with the new value and metadata.
    *   **`retrieve()`:** Attempts to fetch from `cachePolicy.get()`. If a valid (non-expired) entry is found, it's returned. Otherwise, it fetches from the underlying `storage`, adds the result (value and metadata) to the `cachePolicy`, and then returns it.
    *   **`delete()`:** Deletes from the underlying `storage`. On success, it removes the corresponding entry from `cachePolicy`.
    *   **`exists()`:** Checks `cachePolicy.contains()` first. If not found (or expired), it checks the underlying `storage`.
    *   **`updateMetadata()`:** Updates metadata in the underlying `storage`. On success, if the key exists in the cache, it updates the metadata part of the cached `Pair`.
    *   **`listKeys()`:** This operation is **not** cached and directly delegates to the underlying `storage.listKeys()`.
*   **Cache-Specific Methods:**
    *   `suspend fun clearCache(): Boolean`: Clears the cache via `cachePolicy.clear()`.
    *   `suspend fun maintenance(): Boolean`: Triggers `cachePolicy.maintenance()`.
    *   `suspend fun cacheSize(): Int`: Returns `cachePolicy.size()`.
    *   `suspend fun cacheMaxSize(): Int`: Returns `cachePolicy.maxSize()`.

A conceptual diagram of the caching subsystem:

```mermaid
classDiagram
    direction LR

    package "io.github.solaceharmony.core.storage" {
        interface "Storage<K, V>" {
            <<Interface>>
        }
    }

    package "io.github.solaceharmony.core.storage.cache" {
        interface "CachePolicy<K, V_CACHE>" {
            <<Interface>>
            +add(key: K, value: V_CACHE): Boolean
            +get(key: K): V_CACHE?
            +remove(key: K): Boolean
            +contains(key: K): Boolean
            +clear(): Boolean
            +size(): Int
            +maxSize(): Int
            +maintenance(): Boolean
        }

        class "LRUCachePolicy<K, V_CACHE>" {
            +LRUCachePolicy(maxSize: Int)
        }
        "CachePolicy" <|-- "LRUCachePolicy"

        class "TTLCachePolicy<K, V_CACHE>" {
            +TTLCachePolicy(ttl: Duration, maxSize: Int)
        }
        "CachePolicy" <|-- "TTLCachePolicy"

        class "CachedStorage<K, V_STORAGE>" {
            -storage: Storage<K, V_STORAGE>
            -cachePolicy: CachePolicy<K, Pair<V_STORAGE, Map<String, Any>>>
            +CachedStorage(storage, cachePolicy)
            +clearCache(): Boolean
            +maintenance(): Boolean
            +cacheSize(): Int
            +cacheMaxSize(): Int
        }
        "Storage<K, V_STORAGE>" <|-- "CachedStorage<K, V_STORAGE>"
        "CachedStorage" o-- "Storage" : decorates
        "CachedStorage" o-- "CachePolicy" : uses
    }
    note for "CachedStorage" "V_STORAGE is the type for the underlying storage,\nV_CACHE for CachePolicy here is Pair<V_STORAGE, Map>"
```
This caching layer provides a significant performance optimization opportunity by reducing load on the primary storage backends, with configurable eviction strategies.
#### 3.1.5. Actor State Recovery Subsystem (`io.github.solaceharmony.core.storage.recovery`)
The `storage` module includes a dedicated subsystem for managing the snapshotting and recovery of actor states, ensuring data resilience. This is located in the `io.github.solaceharmony.core.storage.recovery` package.

##### 3.1.5.1. `ActorStateSnapshot` Data Class
This data class represents an immutable snapshot of an actor's complete state at a particular point in time.

*   **Purpose:** To encapsulate all necessary information for restoring an actor to a previous state.
*   **Key Properties:**
    *   `actorId: String`: The unique identifier of the actor.
    *   `actorName: String`: The human-readable name of the actor.
    *   `state: ActorState`: The core state of the actor (e.g., Initialized, Running, Stopped, Error, Paused), referencing the `ActorState` sealed class from `io.github.solaceharmony.core.actor`.
    *   `ports: Map<String, Map<String, Any>>`: Configuration of the actor's communication ports.
    *   `metrics: Map<String, Any>`: Metrics associated with the actor.
    *   `customState: Map<String, Any>`: Any additional custom state data for the actor.
    *   `version: Int`: A version number for the snapshot, typically incrementing.
    *   `timestamp: Long`: The epoch milliseconds timestamp when the snapshot was created.
*   **Builder Pattern:**
    *   A companion object `ActorStateSnapshot.builder(actorId: String)` provides an `ActorStateSnapshotBuilder` instance.
    *   The `ActorStateSnapshotBuilder` class offers a fluent API (`withName()`, `withState()`, etc.) to construct `ActorStateSnapshot` objects.

##### 3.1.5.2. `RecoverableActorStateStorage` Interface
This interface extends `ActorStateStorage` to add functionalities specifically for managing actor state snapshots.

*   **Purpose:** To define a contract for storage backends that can persist and retrieve actor state snapshots.
*   **Inheritance:** Extends `io.github.solaceharmony.core.storage.ActorStateStorage`.
*   **Key Snapshot-Specific Methods:**
    *   `suspend fun createSnapshot(actorId: String): ActorStateSnapshot?`: Implementations are expected to capture the current state of the actor (identified by `actorId`) from the storage and persist it as a new snapshot.
    *   `suspend fun restoreFromSnapshot(snapshot: ActorStateSnapshot): Boolean`: Restores the actor's state in the persistent storage to match the provided `snapshot`. This means the underlying `ActorStateStorage` will reflect the data within the snapshot.
    *   `suspend fun listSnapshots(actorId: String): List<ActorStateSnapshot>`: Retrieves all stored snapshots for a given actor, typically sorted by timestamp.
    *   `suspend fun getLatestSnapshot(actorId: String): ActorStateSnapshot?`: (Default implementation provided) Retrieves the most recent snapshot for an actor.
    *   `suspend fun getSnapshotByVersion(actorId: String, version: Int): ActorStateSnapshot?`: (Default implementation provided) Retrieves a specific snapshot by its version number.
    *   `suspend fun deleteSnapshot(actorId: String, version: Int): Boolean`: Deletes a specific version of an actor's snapshot.
    *   `suspend fun deleteAllSnapshots(actorId: String): Boolean`: Deletes all snapshots associated with a particular actor.

##### 3.1.5.3. `ActorRecoveryManager` Class
This class provides a higher-level API to orchestrate actor snapshotting and recovery processes.

*   **Constructor:** `ActorRecoveryManager(private val storage: RecoverableActorStateStorage)`
*   **Key Functionalities:**
    *   **Snapshot Creation (`createSnapshot(actor: Actor)`):**
        1.  Gathers current data for the live `actor` (ID, name, state) and from the `storage` (ports, metrics, custom state).
        2.  Determines the next snapshot `version` by checking the latest existing snapshot for that actor.
        3.  Builds an `ActorStateSnapshot` object using the builder.
        4.  Persists the new snapshot by calling `storage.restoreFromSnapshot(snapshot)`. *Note: The act of "restoring" a newly created snapshot is the mechanism used here to save it.*
    *   **Actor Restoration (`restoreActor(snapshot, actorFactory)`):**
        1.  Calls `storage.restoreFromSnapshot(snapshot)` to ensure the persistent state reflects the snapshot.
        2.  Uses the provided `actorFactory: (String, String) -> Actor` lambda to instantiate a new live `Actor` object.
        3.  Applies the `snapshot.state` to the live actor instance (e.g., by calling `actor.start()`, `actor.pause(reason)`).
    *   **Bulk Recovery (`recoverAllActors(actorFactory)`):**
        1.  Retrieves all actor IDs from `storage.listKeys()`.
        2.  For each actor ID, fetches the `storage.getLatestSnapshot()`.
        3.  Uses its `restoreActor()` method to recreate and restore each actor from its latest snapshot.
    *   **Snapshot Management:** Provides convenience methods that delegate to the underlying `storage` for listing, retrieving by version, and deleting snapshots.

```mermaid
classDiagram
    direction LR

    package "io.github.solaceharmony.core.actor" {
        class Actor {
            +id: String
            +name: String
            +state: ActorState
            +start()
            +stop()
            +pause(reason: String)
        }
        class ActorState {
            <<Sealed>>
        }
    }

    package "io.github.solaceharmony.core.storage" {
        interface ActorStateStorage {
            <<Interface>>
            +getActorPorts(actorId: String): Map?
            +getActorMetrics(actorId: String): Map?
            +getActorCustomState(actorId: String): Map?
        }
    }

    package "io.github.solaceharmony.core.storage.recovery" {
        class ActorStateSnapshot {
            +actorId: String
            +actorName: String
            +state: ActorState
            +ports: Map
            +metrics: Map
            +customState: Map
            +version: Int
            +timestamp: Long
            +static builder(actorId: String): ActorStateSnapshotBuilder
        }

        class ActorStateSnapshotBuilder {
            +withName(name: String): Self
            +withState(state: ActorState): Self
            +build(): ActorStateSnapshot
        }
        ActorStateSnapshotBuilder ..> ActorStateSnapshot : creates

        interface RecoverableActorStateStorage {
            <<Interface>>
            +createSnapshot(actorId: String): ActorStateSnapshot?
            +restoreFromSnapshot(snapshot: ActorStateSnapshot): Boolean
            +listSnapshots(actorId: String): List<ActorStateSnapshot>
            +getLatestSnapshot(actorId: String): ActorStateSnapshot?
            +getSnapshotByVersion(actorId: String, version: Int): ActorStateSnapshot?
            +deleteSnapshot(actorId: String, version: Int): Boolean
            +deleteAllSnapshots(actorId: String): Boolean
        }
        ActorStateStorage <|-- RecoverableActorStateStorage

        class ActorRecoveryManager {
            -storage: RecoverableActorStateStorage
            +ActorRecoveryManager(storage)
            +createSnapshot(actor: Actor): ActorStateSnapshot?
            +restoreActor(snapshot: ActorStateSnapshot, actorFactory): Actor?
            +recoverAllActors(actorFactory): Map<String, Actor>
            +getLatestSnapshot(actorId: String): ActorStateSnapshot?
        }
        ActorRecoveryManager o-- RecoverableActorStateStorage : uses
        ActorRecoveryManager ..> Actor : uses actorFactory to create
        ActorRecoveryManager ..> ActorStateSnapshot : creates & uses
        RecoverableActorStateStorage ..> ActorStateSnapshot : manages
    }
This recovery system provides a crucial layer of fault tolerance for actors by allowing their states to be periodically saved and restored.
#### 3.1.7. File-Based Storage Implementations (JVM-Specific)
For persistent storage on the JVM, SolaceCore provides file-system-based implementations of the storage interfaces. These are located in the `io.github.solaceharmony.core.storage` package within the `jvmMain` source set.

##### 3.1.7.1. `FileStorage<K, V>` Class
This open class is the base for generic, file-based key-value storage.

*   **Implements:** `Storage<K, V>`.
*   **Constructor:** `FileStorage(baseDirectory, keySerializer, valueSerializer, valueDeserializer)`
    *   `baseDirectory: String`: The root directory where data will be stored.
    *   Optional lambdas are provided for serializing keys to strings (for filenames) and for serializing values to/from `Map<String, Any>` (which are then stored as JSON). Default serializers handle basic cases and direct map storage.
*   **Storage Mechanism:**
    *   Data is stored in a `storage` subdirectory within the `baseDirectory`.
    *   Each entry (`key`, `value`, `metadata`) results in two files:
        *   `{keySerializer(key)}.json`: Stores the JSON representation of the `value` (after being converted to a map by `valueSerializer`).
        *   `{keySerializer(key)}.metadata.json`: Stores the JSON representation of the `metadata` map.
    *   Uses `kotlinx.serialization.json.Json` for JSON operations.
*   **Caching:** Includes an internal `ConcurrentHashMap` to cache retrieved values and their metadata, reducing disk I/O for subsequent reads. A `clearCache()` method is provided.
*   **Thread Safety:** File I/O operations are performed on `Dispatchers.IO`. Cache access is synchronized with a `Mutex`.

##### 3.1.7.2. `TransactionalFileStorage<K, V>` Class
Extends `FileStorage` to add transactional support for file-based persistence.

*   **Implements:** `TransactionalStorage<K, V>`.
*   **Inheritance:** Extends `FileStorage<K, V>`.
*   **Transactional Logic:**
    *   Manages an active transaction state (`transactionActive`) and uses a `transactionMutex`.
    *   Pending changes (stores, updates, deletes) within a transaction are held in in-memory `ConcurrentHashMap` structures (`transactionStorage`, `transactionDeletes`).
    *   **`commit()`:** Applies the pending changes directly to the file system by calling the parent `FileStorage`'s `store()` and `delete()` methods for each modified entry. If any file operation fails during commit, it attempts to `rollback()` (which clears the in-memory transaction state).
    *   **`rollback()`:** Clears the in-memory transaction data, discarding pending changes.
    *   Read operations (`retrieve`, `listKeys`, `exists`) consult the in-memory transactional state first before falling back to the `FileStorage` methods if a transaction is active.
    *   A `transaction` subdirectory within `baseDirectory` is created but appears unused for staging files in the current implementation; commits write directly to the main storage files.

##### 3.1.7.3. `FileActorStateStorage` Class
A file-based implementation for `ActorStateStorage`.

*   **Implements:** `ActorStateStorage`.
*   **Inheritance:** Extends `FileStorage<String, Map<String, Any>>`.
*   **Functionality:**
    *   Uses the underlying `FileStorage` to save each actor's entire state (a `Map<String, Any>`) as a single JSON file, keyed by `actorId`.
    *   Specialized methods like `getActorState`, `setActorPorts`, etc., operate by reading the actor's main JSON file into a map, modifying the relevant nested parts (e.g., the "state" or "ports" keys), and then writing the entire map back to the file.
    *   Handles serialization/deserialization of the `ActorState` enum to/from its map representation internally.

##### 3.1.7.4. `FileConfigurationStorage` Class
A file-based implementation for `ConfigurationStorage`.

*   **Implements:** `ConfigurationStorage`.
*   **Inheritance:** Extends `FileStorage<String, Map<String, Any>>`.
*   **Functionality:**
    *   Stores entire configuration maps as JSON files (e.g., `component:myComponentId.json`, `system.json`).
    *   Path-based access methods (`getConfigValue`, `setConfigValue`) read the relevant JSON file into a map, navigate/modify the nested structure based on the dot-separated path, and then write the entire map back to the file.

##### 3.1.7.5. `FileStorageManager` Class
The file-based implementation of the central `StorageManager`.

*   **Implements:** `StorageManager`.
*   **Constructor:** `FileStorageManager(private val baseDirectory: String)`
*   **Functionality:**
    *   Creates the `baseDirectory` if it doesn't exist.
    *   Directly instantiates `FileConfigurationStorage` and `FileActorStateStorage` using the `baseDirectory`.
    *   Manages other registered generic `Storage` instances (expected to be `FileStorage` or `TransactionalFileStorage`) in an internal map.
    *   `flushAll()` is a no-op, as file writes are generally considered immediate at this level.
    *   `clearAll()`: Clears the internal cache of any managed `FileStorage` instances and then recursively deletes all files and subdirectories within the "storage" subdirectories of its `baseDirectory`.
    *   `dispose()`: Calls `stop()` but, unlike the in-memory version, does not clear data from disk, preserving persistence.

These file-based implementations provide durable storage options for SolaceCore on the JVM, suitable for scenarios requiring data to persist across application restarts.
#### 3.1.8. Storage Compression Subsystem (JVM-Specific)
The `io.github.solaceharmony.core.storage.compression` package in `jvmMain` provides a mechanism to transparently compress and decompress data being persisted through the `Storage` interface.

##### 3.1.8.1. `CompressionStrategy` Interface
This interface defines the contract for various compression and serialization algorithms.

*   **Purpose:** To allow pluggable strategies for data compression and the necessary serialization/deserialization steps before/after compression.
*   **Key Methods:**
    *   `fun compress(data: ByteArray): ByteArray`: Compresses the input byte array.
    *   `fun decompress(data: ByteArray): ByteArray`: Decompresses the input byte array.
    *   `fun serialize(value: Any): ByteArray`: Converts an arbitrary object into a byte array suitable for compression.
    *   `fun <T> deserialize(data: ByteArray, clazz: Class<T>): T`: Converts a byte array (typically after decompression) back into an object of type `T`, requiring the `Class<T>` due to JVM type erasure.

##### 3.1.8.2. `GZIPCompressionStrategy` Class
A concrete implementation of `CompressionStrategy` using the GZIP algorithm.

*   **Compression/Decompression:** Uses `java.util.zip.GZIPOutputStream` and `java.util.zip.GZIPInputStream`. The `compress` method only returns compressed data if it's smaller than the original.
*   **Serialization/Deserialization:**
    *   Uses `kotlinx.serialization.json.Json` (configured with `ignoreUnknownKeys = true`, `isLenient = true`).
    *   `serialize(value: Any)`: Handles `ByteArray`, `String`, and primitive types directly. For `Map<*, *>` it builds a `JsonObject`. Other types are attempted to be JSON serialized directly; on failure, it falls back to serializing `value.toString()` (potentially wrapped).
    *   `deserialize<T>(data: ByteArray, clazz: Class<T>)`: Handles `ByteArray`, `String`, and primitives. For `Map`, it manually parses the JSON. For other types, it attempts to deserialize a wrapped string or, as a last resort, returns the raw string if `clazz` is `String`.

##### 3.1.8.3. `CompressedStorage<K, V>` Class
A decorator class that wraps an existing `Storage<K, V>` implementation to add compression capabilities.

*   **Implements:** `Storage<K, V>`.
*   **Constructor:** `CompressedStorage<K, V>(storage: Storage<K, V>, compressionStrategy: CompressionStrategy = GZIPCompressionStrategy(), compressionThreshold: Int = 1024, valueClass: Class<V>)`
    *   `storage`: The underlying storage instance.
    *   `compressionStrategy`: The strategy for compression/decompression and serialization/deserialization (defaults to `GZIPCompressionStrategy`).
    *   `compressionThreshold`: Values (in bytes, after serialization) smaller than this threshold will not be compressed (default 1KB).
    *   `valueClass: Class<V>`: Required for type-safe deserialization by the `CompressionStrategy`.
*   **Operation:**
    *   **`store()`:**
        1.  Serializes the value using `compressionStrategy.serialize()`.
        2.  If the serialized size meets the `compressionThreshold`, it compresses the data using `compressionStrategy.compress()`.
        3.  Stores special metadata keys: `COMPRESSED_KEY: Boolean` and `ORIGINAL_SIZE_KEY: Int`.
        4.  Delegates to the underlying `storage.store()` with the (potentially compressed) value and augmented metadata.
    *   **`retrieve()`:**
        1.  Retrieves data and metadata from the underlying `storage`.
        2.  Checks the `COMPRESSED_KEY` in metadata.
        3.  If compressed, it decompresses using `compressionStrategy.decompress()` and then deserializes using `compressionStrategy.deserialize(decompressedData, valueClass)`.
    *   Other `Storage` methods (`listKeys`, `delete`, `exists`) largely delegate to the underlying storage, with `updateMetadata` taking care to preserve compression-related metadata.
*   **Additional Functionality:** Provides methods like `getCompressionRatio(key)` to inspect compression effectiveness.
*   **Thread Safety:** Uses a `Mutex` for compression-related operations and `Dispatchers.IO` for underlying storage calls.

```mermaid
classDiagram
    direction LR

    package "io.github.solaceharmony.core.storage" {
        interface "Storage<K, V>" { <<Interface>> }
    }

    package "io.github.solaceharmony.core.storage.compression" {
        interface CompressionStrategy {
            <<Interface>>
            +compress(data: ByteArray): ByteArray
            +decompress(data: ByteArray): ByteArray
            +serialize(value: Any): ByteArray
            +deserialize(data: ByteArray, clazz: Class<T>): T
        }

        class GZIPCompressionStrategy {
            +compress(data: ByteArray): ByteArray
            +decompress(data: ByteArray): ByteArray
            +serialize(value: Any): ByteArray
            +deserialize(data: ByteArray, clazz: Class<T>): T
        }
        CompressionStrategy <|-- GZIPCompressionStrategy

        class "CompressedStorage<K, V>" {
            -storage: Storage<K, V>
            -compressionStrategy: CompressionStrategy
            -compressionThreshold: Int
            -valueClass: Class<V>
            +store(key: K, value: V, metadata: Map): Boolean
            +retrieve(key: K): Pair<V, Map>?
        }
        "Storage<K, V>" <|-- "CompressedStorage<K, V>"
        "CompressedStorage" o-- "Storage" : decorates
        "CompressedStorage" o-- CompressionStrategy : uses
    }
```
This compression layer allows for efficient storage of large data by transparently applying compression based on configurable strategies and thresholds.
#### 3.1.9. Storage Encryption Subsystem (JVM-Specific)
SolaceCore provides a robust mechanism for encrypting data at rest within its storage module. This subsystem, located in the `io.github.solaceharmony.core.storage.encryption` package in `jvmMain`, ensures the confidentiality and integrity of stored values and their metadata.

##### 3.1.9.1. `EncryptionStrategy` Interface
This interface defines the fundamental contract for encryption and decryption operations, allowing for different cryptographic algorithms to be used.

*   **Purpose:** To abstract the specific encryption algorithm, enabling pluggable encryption strategies.
*   **Key Methods:**
    *   `fun encrypt(data: ByteArray): ByteArray`: Takes raw byte data and returns its encrypted form.
    *   `fun decrypt(data: ByteArray): ByteArray`: Takes encrypted byte data and returns its decrypted (original) form.

##### 3.1.9.2. `AESEncryptionStrategy` Class
A concrete implementation of `EncryptionStrategy` utilizing the Advanced Encryption Standard (AES).

*   **Algorithm:** Employs AES in Galois/Counter Mode (GCM) with no padding (`AES/GCM/NoPadding`). GCM provides both encryption and authentication (AEAD - Authenticated Encryption with Associated Data).
*   **Key Management:**
    *   Constructor: `AESEncryptionStrategy(private val key: SecretKey = generateKey())`. It can accept a `javax.crypto.SecretKey` or generate a 256-bit AES key by default.
    *   Companion object provides utilities: `generateKey()`, `createKeyFromBytes(keyBytes: ByteArray)`, and `createKeyFromBase64(keyBase64: String)`.
*   **Encryption Process (`encrypt()`):**
    1.  Generates a random 12-byte Initialization Vector (IV) required for GCM mode.
    2.  Initializes an AES cipher for encryption using the provided key, the generated IV, and a GCM tag length of 128 bits.
    3.  Encrypts the input data.
    4.  Returns a byte array containing the IV prepended to the ciphertext (`IV + Ciphertext`).
*   **Decryption Process (`decrypt()`):**
    1.  Extracts the 12-byte IV from the beginning of the input data.
    2.  Initializes the AES cipher for decryption using the key, the extracted IV, and the 128-bit GCM tag length.
    3.  Decrypts the remaining portion of the input data (the ciphertext).
    4.  GCM mode inherently verifies the authenticity tag during decryption, throwing an exception if the data has been tampered with or the key/IV is incorrect.

##### 3.1.9.3. `EncryptedStorage<K, V>` Class
A decorator class that wraps an existing `Storage` implementation to provide transparent encryption and decryption of stored data.

*   **Implements:** `Storage<K, V>`.
*   **Constructor:** `EncryptedStorage(storage: Storage<K, ByteArray>, encryptionStrategy: EncryptionStrategy, valueSerializer: (V) -> String, valueDeserializer: (String) -> V)`
    *   `storage: Storage<K, ByteArray>`: The crucial point here is that the underlying storage **must** be capable of storing `ByteArray` values, as the encrypted content is a byte array.
    *   `encryptionStrategy: EncryptionStrategy`: The strategy used for cryptographic operations (e.g., an instance of `AESEncryptionStrategy`).
    *   `valueSerializer: (V) -> String`: A lambda function to serialize the original value of type `V` into a JSON string before it's encrypted. Defaults to a generic JSON serialization.
    *   `valueDeserializer: (String) -> V`: A lambda function to deserialize a JSON string (obtained after decryption) back into an object of type `V`. Defaults to a generic JSON deserialization.
*   **Operation:**
    *   **`store(key, value, metadata)`:**
        1.  The `value` (type `V`) is serialized to a JSON string using `valueSerializer`.
        2.  The `metadata` (type `Map<String, Any>`) is serialized to a JSON string.
        3.  These two JSON strings are combined into a single JSON object structure (e.g., `{"value": "...", "metadata": "..."}`).
        4.  This combined JSON string is converted to a `ByteArray`.
        5.  The byte array is encrypted using `encryptionStrategy.encrypt()`.
        6.  The resulting encrypted `ByteArray` is stored in the underlying `storage` instance (which is of type `Storage<K, ByteArray>`).
    *   **`retrieve(key)`:**
        1.  Retrieves the encrypted `ByteArray` from the underlying `storage`.
        2.  Decrypts it using `encryptionStrategy.decrypt()`.
        3.  Converts the decrypted byte array back to the combined JSON string.
        4.  Parses this JSON to extract the original value's JSON string and the metadata's JSON string.
        5.  Deserializes the value's JSON string back to type `V` using `valueDeserializer`.
        6.  Deserializes the metadata's JSON string back to `Map<String, Any>`.
        7.  Returns the deserialized `value` and `metadata`.
    *   Other methods like `listKeys()`, `delete()`, and `exists()` primarily delegate to the underlying storage, as keys themselves are not encrypted by this wrapper. `updateMetadata` involves a decrypt-update-encrypt cycle.
*   **Thread Safety:** Uses separate `Mutex` instances for `store`, `retrieve`, and `updateMetadata` operations to manage concurrent access, though cryptographic operations and JSON serialization/deserialization are often performed outside these specific storage locks.

```mermaid
classDiagram
    direction LR

    package "io.github.solaceharmony.core.storage" {
        interface "Storage<K, V_OUT>" { <<Interface>> }
    }

    package "io.github.solaceharmony.core.storage.encryption" {
        interface EncryptionStrategy {
            <<Interface>>
            +encrypt(data: ByteArray): ByteArray
            +decrypt(data: ByteArray): ByteArray
        }

        class AESEncryptionStrategy {
            -key: SecretKey
            +AESEncryptionStrategy(key: SecretKey)
            +encrypt(data: ByteArray): ByteArray
            +decrypt(data: ByteArray): ByteArray
        }
        EncryptionStrategy <|-- AESEncryptionStrategy

        class "EncryptedStorage<K, V_APP>" {
            -storage: Storage<K, ByteArray>  // Underlying storage takes byte arrays
            -encryptionStrategy: EncryptionStrategy
            -valueSerializer: (V_APP) -> String
            -valueDeserializer: (String) -> V_APP
            +store(key: K, value: V_APP, metadata: Map): Boolean
            +retrieve(key: K): Pair<V_APP, Map>?
        }
        "Storage<K, V_APP>" <|-- "EncryptedStorage<K, V_APP>"
        "EncryptedStorage" o-- "Storage" : decorates (specifically Storage<K, ByteArray>)
        "EncryptedStorage" o-- EncryptionStrategy : uses
    }
    note for "EncryptedStorage" "V_APP is the application-level value type.\nInternally, it's serialized to JSON String, then to ByteArray, then encrypted."
```
This encryption layer provides a robust mechanism for securing sensitive data within the storage system, ensuring that both values and their metadata are protected.
#### 3.1.10. JVM-Specific Serialization Utilities (`storage/serialization` Subdirectory)
The `io.github.solaceharmony.core.storage.serialization` package in `jvmMain` contains utility classes for serialization on the JVM.

##### 3.1.10.1. `SerializationWrapper` Data Class
This data class provides a fallback mechanism for serializing objects that may not have explicit support from `kotlinx.serialization` or a custom registered serializer.

*   **Purpose:** To wrap an object's string representation (`toString()`) for serialization, typically when direct serialization of the object itself is not feasible or fails.
*   **Definition:**
    ```kotlin
    package io.github.solaceharmony.core.storage.serialization

    import kotlinx.serialization.Serializable

    @Serializable
    data class SerializationWrapper(val value: String)
    ```
*   **Usage:**
    *   It is used, for example, in `GZIPCompressionStrategy` as a last resort to serialize an object by taking its `toString()` output and storing it in the `value` field of `SerializationWrapper`.
    *   This wrapper can then be easily serialized to JSON (e.g., `{"value": "object_as_string"}`).
    *   Upon deserialization, one would retrieve the original object's string representation from the `value` property. This does not reconstruct the original object instance but provides its string form.

This utility ensures that a string representation can almost always be persisted, even for complex or non-standard objects, albeit with the loss of the original object's type and structure beyond its string form.
#### 3.1.6. Actor State Serialization Subsystem (`io.github.solaceharmony.core.storage.serialization`)
To handle the specific needs of persisting complex `ActorState` objects and custom actor data, the `storage` module includes a dedicated serialization subsystem. This is found in the `io.github.solaceharmony.core.storage.serialization` package.

##### 3.1.6.1. `ActorStateSerializer<T : Any>` Interface
This interface defines a contract for serializers specifically designed for actor state components.

*   **Purpose:** To provide a standardized way to convert actor state related objects to and from a `Map<String, Any>` representation, suitable for storage.
*   **Generic:** `T` - The type of object the serializer handles.
*   **Key Methods:**
    *   `fun serialize(obj: T): Map<String, Any>`: Converts the object `obj` into a map.
    *   `fun deserialize(map: Map<String, Any>): T?`: Converts a map back into an object of type `T`, returning `null` on failure.
    *   `fun getType(): KClass<T>`: Returns the `KClass` of the object type `T` this serializer is responsible for.

##### 3.1.6.2. `ActorStateEnumSerializer` Class
A concrete implementation of `ActorStateSerializer<ActorState>` specifically for the `io.github.solaceharmony.core.actor.ActorState` sealed class.

*   **Purpose:** To correctly serialize and deserialize the different states of an actor (e.g., `Initialized`, `Running`, `Stopped`, `Error`, `Paused`), including any associated data like error messages or pause reasons.
*   **Serialization Logic:** Converts `ActorState` instances into a map, typically including a "type" field (e.g., "Running") and other relevant fields (e.g., "exception" for `ActorState.Error`).
*   **Deserialization Logic:** Reconstructs the appropriate `ActorState` instance based on the "type" field and other data in the input map.

##### 3.1.6.3. `ActorStateSerializerRegistry` Class
Manages a collection of `ActorStateSerializer` instances.

*   **Purpose:** To act as a central point for registering and retrieving serializers for different actor state component types.
*   **Internal Structure:** Uses a `mutableMapOf<KClass<*>, ActorStateSerializer<*>>` to store serializers, keyed by the `KClass` they handle.
*   **Key Methods:**
    *   `fun <T : Any> registerSerializer(serializer: ActorStateSerializer<T>)`
    *   `fun <T : Any> getSerializer(clazz: KClass<T>): ActorStateSerializer<T>?`
    *   `fun <T : Any> hasSerializer(clazz: KClass<T>): Boolean`
    *   `fun <T : Any> unregisterSerializer(clazz: KClass<T>): Boolean`
    *   `fun getAllSerializers(): Map<KClass<*>, ActorStateSerializer<*>>`

##### 3.1.6.4. `SerializableActorStateStorage` Interface
Extends `ActorStateStorage` to integrate specialized serialization for custom actor data.

*   **Purpose:** To provide methods for storing and retrieving typed objects within an actor's "customState" map, using registered `ActorStateSerializer`s.
*   **Inheritance:** Extends `io.github.solaceharmony.core.storage.ActorStateStorage`.
*   **Key Added Methods:**
    *   `fun getSerializerRegistry(): ActorStateSerializerRegistry`: Provides access to the associated serializer registry.
    *   `suspend fun <T : Any> serializeAndStore(actorId: String, key: String, obj: T, clazz: KClass<T>): Boolean`:
        1.  Retrieves the appropriate `ActorStateSerializer<T>` for `clazz` from the registry.
        2.  Serializes `obj` into a map.
        3.  Retrieves the actor's current data (or an empty map if new).
        4.  Stores the serialized map under the given `key` within the actor's "customState" field (which is itself a map).
        5.  Persists the updated actor data using the underlying `store` method.
    *   `suspend fun <T : Any> retrieveAndDeserialize(actorId: String, key: String, clazz: KClass<T>): T?`:
        1.  Retrieves the actor's data.
        2.  Accesses the "customState" map, then the serialized map under the given `key`.
        3.  Retrieves the appropriate `ActorStateSerializer<T>` for `clazz`.
        4.  Deserializes the map back into an object of type `T`.

##### 3.1.6.5. `DelegatingSerializableActorStateStorage` Class
A concrete implementation of `SerializableActorStateStorage` that uses the decorator pattern.

*   **Purpose:** To provide a ready-to-use `SerializableActorStateStorage` by wrapping an existing `ActorStateStorage` implementation and an `ActorStateSerializerRegistry`.
*   **Constructor:** `DelegatingSerializableActorStateStorage(private val delegate: ActorStateStorage, private val serializerRegistry: ActorStateSerializerRegistry = ActorStateSerializerRegistry())`
*   **Functionality:**
    *   Delegates all standard `ActorStateStorage` methods to the `delegate` instance.
    *   Implements the `serializeAndStore` and `retrieveAndDeserialize` methods using its `serializerRegistry`.
    *   The `init` block automatically registers an `ActorStateEnumSerializer` with its `serializerRegistry`.
*   **Factory Method:** A companion object provides `createInMemory(serializerRegistry: ActorStateSerializerRegistry = ...)` which conveniently creates an instance delegating to a new `InMemoryActorStateStorage`.

```mermaid
classDiagram
    direction LR

    package "io.github.solaceharmony.core.actor" {
        class ActorState { <<Sealed>> }
    }

    package "io.github.solaceharmony.core.storage" {
        interface ActorStateStorage { <<Interface>> }
        class InMemoryActorStateStorage { }
        ActorStateStorage <|-- InMemoryActorStateStorage
    }

    package "io.github.solaceharmony.core.storage.serialization" {
        interface "ActorStateSerializer<T>" {
            <<Interface>>
            +serialize(obj: T): Map
            +deserialize(map: Map): T?
            +getType(): KClass<T>
        }

        class ActorStateEnumSerializer {
            +serialize(obj: ActorState): Map
            +deserialize(map: Map): ActorState?
            +getType(): KClass<ActorState>
        }
        "ActorStateSerializer" <|-- ActorStateEnumSerializer
        ActorStateEnumSerializer ..> ActorState

        class ActorStateSerializerRegistry {
            -serializers: Map<KClass, ActorStateSerializer>
            +registerSerializer(serializer: ActorStateSerializer)
            +getSerializer(clazz: KClass): ActorStateSerializer?
        }
        ActorStateSerializerRegistry o-- "ActorStateSerializer"

        interface SerializableActorStateStorage {
            <<Interface>>
            +getSerializerRegistry(): ActorStateSerializerRegistry
            +serializeAndStore(actorId, key, obj, clazz): Boolean
            +retrieveAndDeserialize(actorId, key, clazz): Any?
        }
        ActorStateStorage <|-- SerializableActorStateStorage

        class DelegatingSerializableActorStateStorage {
            -delegate: ActorStateStorage
            -serializerRegistry: ActorStateSerializerRegistry
            +init() // registers ActorStateEnumSerializer
        }
        SerializableActorStateStorage <|-- DelegatingSerializableActorStateStorage
        DelegatingSerializableActorStateStorage o-- ActorStateStorage : delegates to
        DelegatingSerializableActorStateStorage o-- ActorStateSerializerRegistry : uses

        DelegatingSerializableActorStateStorage ..> InMemoryActorStateStorage : can create (companion)
    }
```
This specialized serialization framework enhances the storage module by providing robust, type-safe handling for actor state data, particularly for custom objects within an actor's state.

---

← [§2 Lifecycle Module (`io.github.solaceharmony.core.lifecycle`)](Lifecycle-and-Resources)  ·  [Architecture Overview](Architecture-Overview)  ·  [§4 Actor Module (`io.github.solaceharmony.core.actor`)](Actor-System) →
