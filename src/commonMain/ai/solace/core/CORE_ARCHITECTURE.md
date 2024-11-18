# System Architecture

## Project Structure
```
ai.solace.core/
├── channels/              # Channel-based communication
│   ├── Port.kt           # Port definitions
│   ├── InputPort.kt      # Input channel wrapper
│   ├── OutputPort.kt     # Output channel wrapper
│   └── ChannelRegistry.kt # Channel management
├── workflow/             # Workflow management
│   ├── WorkflowBuilder.kt # Workflow construction
│   ├── WorkflowManager.kt # Workflow execution
│   └── Connection.kt     # Connection definitions
└── actor/               # Actor system (as before)
    ├── Actor.kt
    ├── message/
    ├── metrics/
    └── types/
```

## Channel System
```kotlin
// Independent channel system that can work without actors
package ai.solace.core.channels

interface Port<T> {
    val name: String
    val type: KClass<T>
}

class InputPort<T : Any>(
    override val name: String,
    override val type: KClass<T>,
    private val channel: Channel<T>
) : Port<T> {
    suspend fun receive(): T = channel.receive()
}

class OutputPort<T : Any>(
    override val name: String,
    override val type: KClass<T>,
    private val channel: Channel<T>
) : Port<T> {
    suspend fun send(value: T) = channel.send(value)
}
```

## Workflow System
```kotlin
package ai.solace.core.workflow

data class Connection<T : Any>(
    val from: OutputPort<T>,
    val to: InputPort<T>
)

class WorkflowBuilder(private val scope: CoroutineScope) {
    private val nodes = mutableListOf<Any>()  // Can be actors or any other processable node
    private val connections = mutableListOf<Connection<*>>()

    fun connect<T : Any>(
        fromNode: Any,
        fromPort: String,
        toNode: Any,
        toPort: String,
        type: KClass<T>
    ): WorkflowBuilder {
        // Create connection between any nodes with ports
        return this
    }
}
```

## Integration with Actors
```kotlin
// Actors become one type of node that can participate in workflows
class ActorNode(private val actor: Actor) : WorkflowNode {
    override val inputs: List<InputPort<*>> = actor.getInterface().inputs
    override val outputs: List<OutputPort<*>> = actor.getInterface().outputs
}

// Usage
val workflow = WorkflowBuilder(scope)
    .addNode(ActorNode(filterActor))
    .addNode(ActorNode(processActor))
    .connect<String>(
        fromNode = filterActor,
        fromPort = "output",
        toNode = processActor,
        toPort = "input",
        String::class
    )
    .build()
```

The benefits of this reorganization:
1. Channels become a standalone system
2. Workflows can connect any components with ports, not just actors
3. Better separation of concerns
4. More flexible architecture for future extensions
