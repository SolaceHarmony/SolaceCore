<!-- topic: Runtime -->
<!-- title: Storage File-Based Architecture -->

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



[Back to Storage Module Architecture](Storage-Module-Architecture)
