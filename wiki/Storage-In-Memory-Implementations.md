<!-- topic: Runtime -->
<!-- title: Storage In-Memory Implementations -->

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



[Back to Storage & Persistence](Storage-and-Persistence)
