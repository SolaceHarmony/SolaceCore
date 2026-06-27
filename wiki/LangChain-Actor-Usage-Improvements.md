<!-- topic: Reference -->
<!-- title: LangChain Actor Usage Improvements -->

## Core Actor System Usage Issues

### 1. Actor Creation Complexity
**Current Issue:**
```kotlin
// Current approach requires multiple steps
val actor = Actor(id = "my-actor")
actor.defineInterface()
actor.start()
```

**Proposed Solution:**
```kotlin
// Builder pattern with DSL
val actor = buildActor {
    id("my-actor")
    inputs {
        port("data", String::class)
        port("control", ControlMessage::class)
    }
    outputs {
        port("result", ProcessedData::class)
        port("metrics", ActorMetrics::class)
    }
    processing {
        onMessage { msg ->
            when (msg.type) {
                "data" -> processData(msg)
                "control" -> handleControl(msg)
            }
        }
    }
}
```

### 2. Message Type Safety
**Current Issue:**
```kotlin
// Current approach uses Any for payload
data class ActorMessage(
    val type: String,
    val payload: Any  // Type unsafe
)
```

**Proposed Solution:**
```kotlin
// Type-safe messages
sealed class ActorMessage<T> {
    val correlationId: String = UUID.randomUUID().toString()
    abstract val payload: T
}

data class DataMessage(
    override val payload: ProcessableData
) : ActorMessage<ProcessableData>()

// Usage
actor.send(DataMessage(myData))
```

### 3. Error Handling Verbosity
**Current Issue:**
```kotlin
try {
    actor.send(message)
} catch (e: IllegalStateException) {
    // Handle state error
} catch (e: TimeoutException) {
    // Handle timeout
} catch (e: Exception) {
    // Handle other errors
}
```

**Proposed Solution:**
```kotlin
// Result-based error handling
sealed class ActorResult<T> {
    data class Success<T>(val value: T) : ActorResult<T>()
    data class Error<T>(val error: ActorError) : ActorResult<T>()
}

// Usage
when (val result = actor.sendSafely(message)) {
    is ActorResult.Success -> handleSuccess(result.value)
    is ActorResult.Error -> handleError(result.error)
}
```



[Back to LangChain Usage Design Improvements](LangChain-Usage-Design-Improvements)
