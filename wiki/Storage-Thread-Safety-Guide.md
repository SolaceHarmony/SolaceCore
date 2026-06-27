<!-- topic: Runtime -->
<!-- title: Storage Thread Safety Guide -->

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


[Back to Storage & Persistence](Storage-and-Persistence)
