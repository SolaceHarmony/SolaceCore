<!-- topic: Runtime -->
<!-- title: Storage Module Architecture -->

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

