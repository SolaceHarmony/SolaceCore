<!-- topic: Runtime -->
<!-- title: Storage Core Interfaces -->

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


[Back to Storage & Persistence](Storage-and-Persistence)
