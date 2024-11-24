# Actor System Architecture

## Project Structure
```
ai.solace.core.actor/
├── Actor.kt                 # Base abstract actor class
├── config/
│   └── ActorConfiguration.kt
├── interfaces/
│   ├── ActorInterface.kt    # Defined connection points for Actors
│   ├── ActorMessage.kt     # Message data class
│   └── PortDefinition.kt   # Port definitions for Actors
├── observability/
│   ├── ActorState.kt       # Running state of Actor
│   └── ActorMetrics.kt     # Metrics collection
├── builder/
│   └── ActorBuilder.kt     # Actor construction
└── types/
    ├── KernelActor.kt      # Framework/core actors
    ├── RouterActor.kt      # Router Actor 
    ├── SupervisorActor.kt  # Supervisor Actor
    └── scripted/           # Runtime script actors
        └── ScriptedActor.kt
```

## Class Hierarchy
```
Actor (abstract)
├── KernelActor (abstract)   # Framework/core components
└── ScriptedActor           # Runtime script execution
```

## Core Components

### 1. **Actor Base Class**
```kotlin
@OptIn(ExperimentalUuidApi::class)
abstract class Actor(
    val id: String = Uuid.random().toString(),
    protected val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    protected abstract fun defineInterface()
    protected abstract suspend fun processMessage(message: ActorMessage)
}
```

### 2. **Message System**
```kotlin
@OptIn(ExperimentalUuidApi::class)
data class ActorMessage(
    val correlationId: String = Uuid.random().toString(),
    val type: String,
    val payload: Any,
    val sender: String? = null
)
```

### 3. **Actor Types**

#### Kernel Actors
```kotlin
abstract class KernelActor(id: String) : Actor(id) {
    // Core system functionality
}

// Example kernel actors
class RouterActor(id: String) : KernelActor(id)
class SupervisorActor(id: String) : KernelActor(id)
```

#### Scripted Actors
```kotlin
class ScriptedActor(
    id: String,
    private val scriptEngine: ScriptEngine,
    private val script: String,
) : Actor(id) {
    // Script execution functionality
}
```

### 4. **Builder System**
```kotlin
sealed class ActorBuilder {
    class KernelActorBuilder : ActorBuilder() {
        fun withActorClass(clazz: Class<out KernelActor>)
    }
    
    class ScriptedActorBuilder : ActorBuilder() {
        fun withScript(script: String)
        fun withScriptEngine(engine: ScriptEngine)
    }
    
    companion object {
        fun kernel() = KernelActorBuilder()
        fun scripted() = ScriptedActorBuilder()
    }
}
```

## System Features

### Message Processing
```kotlin
// Sending messages
actor.send(ActorMessage(
    type = "ProcessData",
    payload = data
))

// Processing messages
override suspend fun processMessage(message: ActorMessage) {
    when (message.type) {
        "ProcessData" -> handleData(message.payload)
        "Control" -> handleControl(message.payload)
    }
}
```

### Actor Lifecycle
```kotlin
// Starting an actor
actor.start()

// Stopping an actor
actor.stop()

// State management
when (actor.getState()) {
    ActorState.RUNNING -> // handle running state
    ActorState.STOPPED -> // handle stopped state
}
```

### Metrics Collection
```kotlin
val metrics = actor.metrics.getMetrics()
println("Messages processed: ${metrics["messagesProcessed"]}")
println("Average processing time: ${metrics["averageProcessingTime"]}")
```

## Usage Examples

### Creating Kernel Actors
```kotlin
// Create a router actor
val router = ActorBuilder.kernel()
    .withActorClass(RouterActor::class.java)
    .build()

// Create with specific ID
val supervisor = ActorBuilder.kernel()
    .withId("main-supervisor")
    .withActorClass(SupervisorActor::class.java)
    .build()
```

### Creating Scripted Actors
```kotlin
// Create a flow actor
val flowActor = ActorBuilder.scripted()
    .withScriptEngine(engine)
    .withScript("""
        override fun processMessage(message: ActorMessage) {
            when (message.type) {
                "ProcessFlow" -> {
                    // Flow processing logic
                }
            }
        }
    """)
    .build()
```

## Platform Requirements

### JDK Requirements
- JDK 21
  - Virtual Thread support
  - Enhanced pattern matching
  - String templates

### Kotlin Requirements
- Kotlin 2.0.20+
  - Built-in UUID support
  - Improved coroutines
  - Enhanced type inference

## Best Practices

### 1. Actor Design
- Keep actors focused on single responsibilities
- Use meaningful actor IDs
- Handle all potential message types
- Implement proper error handling

### 2. Message Handling
```kotlin
override suspend fun processMessage(message: ActorMessage) {
    try {
        when (message.type) {
            "Known" -> handleKnown(message)
            else -> handleUnknown(message)
        }
    } catch (e: Exception) {
        handleError(e, message)
    }
}
```

### 3. Resource Management
- Properly clean up resources in stop()
- Use structured concurrency
- Monitor actor health
- Track metrics

## Testing Guidelines
1. Unit test individual actors
2. Test message processing flows
3. Verify state transitions
4. Check error handling
5. Monitor metrics collection

## Future Enhancements
1. Generic type parameters for message payloads
2. Advanced routing capabilities
3. Enhanced metrics and monitoring
4. Multi-instance support
5. Native kernel implementation