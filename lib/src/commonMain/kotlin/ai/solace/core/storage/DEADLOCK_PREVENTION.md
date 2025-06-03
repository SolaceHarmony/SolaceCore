# Deadlock Prevention in SolaceCore Storage System

## Overview

This document outlines best practices for preventing deadlocks in the SolaceCore storage system. Deadlocks can occur when multiple threads attempt to acquire locks in different orders, leading to a situation where each thread is waiting for a lock held by another thread.

## Common Deadlock Scenarios

### Nested Mutex Locks

The most common deadlock scenario in the storage system involves nested mutex locks. This occurs when a method acquires a mutex lock and then calls another method that also tries to acquire the same mutex lock. Since the mutex is already locked, the second method will wait indefinitely for the lock to be released, creating a deadlock.

Example of problematic code:
```kotlin
suspend fun setActorState(actorId: String, state: ActorState): Boolean {
    return mutex.withLock {
        val actorData = retrieve(actorId)?.first?.toMutableMap() ?: mutableMapOf()
        val metadata = retrieve(actorId)?.second?.toMutableMap() ?: mutableMapOf()
        
        // Make changes to actorData
        
        store(actorId, actorData, metadata)
    }
}
```

In this example, both `retrieve()` and `store()` methods internally acquire the same mutex lock, creating a deadlock.

## Best Practices

### 1. Minimize Lock Scope

Only lock the mutex for the minimum time necessary to ensure thread safety. Perform as much work as possible outside the lock.

Good example:
```kotlin
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

### 2. Avoid Nested Locks

Never call a method that acquires the same lock from within a locked block. If you need to call such methods, do so outside the lock.

Bad example:
```kotlin
mutex.withLock {
    // This will cause a deadlock if retrieve() also acquires the same mutex
    val data = retrieve(key)
    // ...
}
```

Good example:
```kotlin
// Call retrieve() outside the lock
val data = retrieve(key)

mutex.withLock {
    // Use the retrieved data inside the lock
    // ...
}
```

### 3. Use Direct Access When Appropriate

In some cases, it may be appropriate to directly access the protected resource instead of calling methods that acquire locks. This should be done carefully and only when necessary.

Example:
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

### 4. Add Error Handling

Add error handling to prevent hanging if something goes wrong. Use try-catch blocks to catch exceptions and release locks properly.

Example:
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

### 5. Use Timeouts for Locks

Consider using timeouts when acquiring locks to prevent indefinite waiting. This can help detect deadlocks during development.

Example:
```kotlin
withTimeout(5.seconds) {
    mutex.withLock {
        // Critical section
    }
}
```

### 6. Document Lock Usage

Document which methods acquire locks and which methods should not be called from within locked blocks. This helps other developers avoid introducing deadlocks.

Example:
```kotlin
/**
 * Retrieves a value with the given key.
 * 
 * Note: This method acquires a mutex lock and should not be called from within a locked block.
 */
suspend fun retrieve(key: K): Pair<V, Map<String, Any>>? {
    return mutex.withLock {
        storage[key]
    }
}
```

## Testing for Deadlocks

Add timeout handling to tests to prevent them from hanging indefinitely if deadlocks occur. This helps catch deadlocks during development.

Example:
```kotlin
@Test
fun testMethod() = runBlocking {
    withTimeout(5.seconds) {
        // Test code that might deadlock
    }
}
```

## Conclusion

Preventing deadlocks requires careful design and attention to lock usage. By following these best practices, you can avoid deadlocks in the SolaceCore storage system and ensure that your code is reliable and efficient.