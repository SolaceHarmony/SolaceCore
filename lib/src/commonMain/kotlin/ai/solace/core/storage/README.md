# SolaceCore Storage System

The SolaceCore Storage System provides a flexible and extensible way to store and retrieve data for the SolaceCore framework. It is designed to handle different types of data, including configuration data and actor state data, and to support different storage backends.

## Architecture

The storage system is built around a set of interfaces that define the contract for storage operations. The core interfaces are:

- `Storage<K, V>`: A generic interface for storing and retrieving data with keys of type `K` and values of type `V`.
- `ConfigurationStorage`: An interface for storing and retrieving configuration data, extending `Storage<String, Map<String, Any>>`.
- `ActorStateStorage`: An interface for storing and retrieving actor state data, extending `Storage<String, Map<String, Any>>`.
- `StorageManager`: An interface for managing different types of storage and providing a unified interface for the storage system.

The storage system also includes serialization support through the `StorageSerializer<T>` interface and the `StorageSerializerRegistry` utility class.

## In-Memory Implementation

The storage system includes in-memory implementations of all the interfaces for development and testing:

- `InMemoryStorage<K, V>`: An in-memory implementation of the `Storage` interface.
- `InMemoryConfigurationStorage`: An in-memory implementation of the `ConfigurationStorage` interface.
- `InMemoryActorStateStorage`: An in-memory implementation of the `ActorStateStorage` interface.
- `InMemoryStorageManager`: An in-memory implementation of the `StorageManager` interface.

These implementations store data in memory and are useful for development and testing, but data is lost when the application is restarted.

## Usage

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

### Custom Storage Types

You can register custom storage implementations for specific key and value types:

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

### Serialization

You can register serializers for custom types to convert them to and from a map representation:

```kotlin
// Create a serializer for MyData
val myDataSerializer = object : StorageSerializer<MyData> {
    override fun serialize(obj: MyData): Map<String, Any> {
        return mapOf("name" to obj.name)
    }

    override fun deserialize(map: Map<String, Any>): MyData {
        return MyData(map["name"] as String)
    }
}

// Register the serializer
StorageSerializerRegistry.registerSerializer(MyData::class.java, myDataSerializer)

// Serialize a MyData object
val myData = MyData("example")
val map = StorageSerializerRegistry.serialize(myData)

// Deserialize a map to a MyData object
val deserializedData = StorageSerializerRegistry.deserialize(map, MyData::class.java)
```

## Future Enhancements

Future enhancements to the storage system may include:

- File-based storage implementations for persistence
- Database storage implementations for scalability
- Distributed storage implementations for clustering
- Encryption support for sensitive data
- Compression support for large data
- Transaction support for atomic operations
- Caching support for improved performance