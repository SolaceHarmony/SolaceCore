<!-- topic: Runtime -->
<!-- title: Storage Abstractions Architecture -->

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


[Back to Storage Module Architecture](Storage-Module-Architecture)
