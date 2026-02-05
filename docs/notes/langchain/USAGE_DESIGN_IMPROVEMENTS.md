# Usage and Design Improvements

This document outlines usage issues and proposed improvements for the SolaceCore framework, organized by package and component.

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

## Port System Usability

### 1. Port Connection Syntax
**Current Issue:**
```kotlin
// Current verbose connection syntax
actor1.getInterface().getOutput("data")
    .connectTo(actor2.getInterface().getInput("data"))
```

**Proposed Solution:**
```kotlin
// Fluent connection API
actor1 connectsTo actor2 {
    "data" to "input"
    "control" to "controlPort"
}

// Or using type-safe references
actor1.outputs.data connectsTo actor2.inputs.data
```

### 2. Port Configuration
**Current Issue:**
```kotlin
// Limited port configuration
interface Port<T> {
    val id: String
    val type: KClass<T>
}
```

**Proposed Solution:**
```kotlin
// Rich port configuration DSL
port("data") {
    type<String>()
    buffered(capacity = 100)
    backpressure = BackpressureStrategy.DROP
    validation { it.length <= 1000 }
    transformation { it.trim() }
}
```

## Lifecycle Management

### 1. Resource Management
**Current Issue:**
```kotlin
// Manual resource tracking
private val resources = mutableListOf<AutoCloseable>()
fun addResource(r: AutoCloseable) {
    resources.add(r)
}
```

**Proposed Solution:**
```kotlin
// Automatic resource management
class Actor : AutoCloseable {
    private val resources = ResourceScope()
    
    init {
        resources.manage {
            autoClose(database)
            autoClose(connection)
            autoDispose(subscription)
        }
    }
}
```

### 2. Startup/Shutdown Sequence
**Current Issue:**
```kotlin
// Manual ordering
override suspend fun start() {
    startDatabase()
    startConnections()
    startProcessing()
}
```

**Proposed Solution:**
```kotlin
// Declarative startup sequence
lifecycle {
    phase("infrastructure") {
        start(database)
        start(metrics)
    }
    phase("connections") {
        start(messageQueue)
        start(eventBus)
    }
    phase("processing") {
        start(messageProcessor)
        start(eventHandler)
    }
}
```

## Testing Improvements

### 1. Actor Testing
**Current Issue:**
```kotlin
// Complex test setup
@Test
fun `test actor processing`() = runTest {
    val actor = TestActor()
    actor.start()
    actor.send(message)
    // Complex verification
}
```

**Proposed Solution:**
```kotlin
// Test DSL
@Test
fun `test actor processing`() = actorTest {
    val actor = testActor {
        processesMessage<DataMessage> { msg ->
            // Processing logic
        }
    }

    actor.receive(DataMessage("test")) {
        expectOutput("result", ProcessedData("test"))
        expectMetric("processing_time") { it < 100.milliseconds }
    }
}
```

### 2. Mock Actor Creation
**Current Issue:**
```kotlin
// Manual mock creation
class MockActor : Actor() {
    override fun processMessage(msg: ActorMessage) {
        // Mock implementation
    }
}
```

**Proposed Solution:**
```kotlin
// Mock DSL
val mockActor = mockActor {
    onMessage("data") { msg ->
        reply(ProcessedData(msg.payload))
    }
    onMessage("control") {
        throw ControlException("test")
    }
}
```

## Configuration Management

### 1. Actor Configuration
**Current Issue:**
```kotlin
// Hard-coded configuration
class MyActor(
    private val bufferSize: Int = 100,
    private val timeout: Duration = 30.seconds
)
```

**Proposed Solution:**
```kotlin
// Type-safe configuration DSL
class MyActor(config: ActorConfig) {
    companion object {
        fun create(block: ActorConfig.() -> Unit): MyActor =
            MyActor(ActorConfig().apply(block))
    }
}

// Usage
val actor = MyActor.create {
    processing {
        bufferSize = 100
        timeout = 30.seconds
        backpressure = DROP
    }
    metrics {
        enabled = true
        samplingRate = 0.1
    }
}
```

## Implementation Priorities

1. **Immediate Improvements**
   - Type-safe message system
   - Builder pattern with DSL
   - Basic error handling improvements

2. **Short-term Goals**
   - Port connection DSL
   - Resource management improvements
   - Testing DSL

3. **Medium-term Goals**
   - Configuration system
   - Metrics enhancements
   - Advanced error handling

4. **Long-term Goals**
   - Full type safety
   - Advanced testing features
   - Performance optimizations

## Migration Strategy

1. **Phase 1: Compatibility Layer**
   ```kotlin
   // Provide extension functions for backward compatibility
   fun Actor.legacyConnect(other: Actor) {
       // Implement old connection logic
   }
   ```

2. **Phase 2: Deprecation**
   ```kotlin
   @Deprecated("Use new DSL instead")
   fun oldMethod() { }
   ```

3. **Phase 3: New API Introduction**
   ```kotlin
   // Introduce new API alongside old one
   class ModernActor : Actor {
       // New implementation
   }
   ```

4. **Phase 4: Complete Migration**
   - Remove deprecated features
   - Update documentation
   - Provide migration guides

## Best Practices

1. **Message Handling**
   ```kotlin
   // Prefer type-safe message handling
   when (val msg = message) {
       is DataMessage -> processData(msg)
       is ControlMessage -> handleControl(msg)
   }
   ```

2. **Resource Management**
   ```kotlin
   // Use structured concurrency
   coroutineScope {
       launch { handleMessages() }
       launch { monitorMetrics() }
   }
   ```

3. **Error Handling**
   ```kotlin
   // Use proper error channels
   actor.errors.collect { error ->
       when (error) {
           is ProcessingError -> handleProcessingError(error)
           is SystemError -> handleSystemError(error)
       }
   }
   ```

## Documentation Improvements

1. **Add Code Examples**
   - Provide complete, runnable examples
   - Include common use cases
   - Show best practices

2. **Interactive Documentation**
   - Create interactive tutorials
   - Provide sandboxed environment
   - Include testing playground

3. **API Guidelines**
   - Document common patterns
   - Explain anti-patterns
   - Provide migration guides

## Notes

- All improvements maintain backward compatibility where possible
- Changes are introduced gradually with proper deprecation cycles
- Documentation is updated in parallel with code changes
- Performance impact is measured for each change

Please update this document as new usage patterns emerge or issues are discovered.