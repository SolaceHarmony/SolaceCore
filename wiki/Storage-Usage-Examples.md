<!-- topic: Runtime -->
<!-- title: Storage Usage Examples -->

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


[Back to Storage & Persistence](Storage-and-Persistence)
