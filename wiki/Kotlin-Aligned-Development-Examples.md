<!-- topic: Reference -->
<!-- title: Kotlin-Aligned Development Examples -->

## Kotlin Development

### Project Structure

```
SolaceCore/
├── lib/                          # Shared library
│   └── src/
│       ├── commonMain/kotlin/    # Platform-agnostic code
│       ├── jvmMain/kotlin/       # JVM-specific code
│       ├── jsMain/kotlin/        # JavaScript-specific code
│       └── nativeMain/kotlin/    # Native-specific code
├── composeApp/                   # UI application
│   └── src/
│       ├── commonMain/kotlin/    # Shared UI logic
│       ├── desktopMain/kotlin/   # Desktop UI
│       └── webMain/kotlin/       # Web UI
└── docs/                        # Documentation
```

### Creating a New Actor

```kotlin
// Define actor messages
sealed class MyActorMessage : ActorMessage() {
    data class ProcessRequest(val input: String) : MyActorMessage()
    data class ProcessingResult(val output: String) : MyActorMessage()
}

// Implement the actor
class MyActor(
    actorId: String,
    supervisor: ActorSupervisor
) : BaseActor(actorId, supervisor) {

    override suspend fun handleMessage(message: ActorMessage) {
        when (message) {
            is MyActorMessage.ProcessRequest -> {
                val result = processInput(message.input)
                sendMessage(message.sender, MyActorMessage.ProcessingResult(result))
            }
            else -> unhandledMessage(message)
        }
    }

    private fun processInput(input: String): String {
        return "Processed: $input"
    }
}
```

### Using the Pipeline DSL

```kotlin
// Create a pipeline using Kotlin DSL
val pipeline = pipeline {
    block("protocol.ollama") {
        "model" to "qwen3-coder:30b"
        "temperature" to 0.7
    }
    block("codec.mcp_over_xml")
    block("tools.negotiation_advertise")
    block("family.qwen3")
}

// Execute the pipeline
val result = pipelineEngine.execute(input, pipeline)
```

### Making a Tool Call via MCP (Example)

```kotlin
val call = McpToolCall(
    name = "list_directory",
    arguments = mapOf("path" to "/tmp"),
    correlationId = UUID.randomUUID().toString()
)

val response = mcpExecutor.execute(call)
if (response.error != null) {
    supervisor.reportError(response.error)
} else {
    neutralHistory.store(
        NeutralEvent(
            id = "tool_${call.correlationId}",
            timestamp = System.currentTimeMillis(),
            type = NeutralEventType.TOOL_RESULT,
            lane = Lane.TECHNICAL,
            source = "main-actor",
            content = NeutralContent(toolName = call.name, result = response.result)
        )
    )
}
```

### Working with Neutral History

```kotlin
// Store an event
val event = NeutralEvent(
    id = "event_${System.currentTimeMillis()}",
    timestamp = System.currentTimeMillis(),
    type = NeutralEventType.MESSAGE,
    lane = Lane.TECHNICAL,
    source = "main-actor",
    content = NeutralContent(text = "User request processed")
)

neutralHistory.store(event)

// Query events
val recentEvents = neutralHistory.retrieve(
    NeutralQuery(
        timeRange = (System.currentTimeMillis() - 3600000)..System.currentTimeMillis(),
        eventTypes = setOf(NeutralEventType.MESSAGE, NeutralEventType.TOOL_USE)
    )
)
```



[Back to Kotlin-Aligned Quick Start](Kotlin-Aligned-Quick-Start)
