<!-- topic: Runtime -->
<!-- title: Storage In-Memory Architecture -->

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


[Back to Storage Module Architecture](Storage-Module-Architecture)
