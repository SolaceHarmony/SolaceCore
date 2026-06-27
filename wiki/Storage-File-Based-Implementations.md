<!-- topic: Runtime -->
<!-- title: Storage File-Based Implementations -->

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


[Back to Storage & Persistence](Storage-and-Persistence)
