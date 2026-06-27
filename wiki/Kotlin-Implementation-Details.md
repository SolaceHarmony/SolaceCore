<!-- topic: Reference -->
<!-- title: Kotlin Implementation Details -->

## Kotlin Implementation Details

### Actor System Implementation

```kotlin
// Actor definition
abstract class Actor(
    val actorId: String,
    val supervisor: ActorSupervisor
) {
    protected val mailbox = Channel<ActorMessage>(capacity = Channel.UNLIMITED)

    suspend fun start() {
        supervisor.register(this)
        messageLoop()
    }

    private suspend fun messageLoop() {
        for (message in mailbox) {
            try {
                handleMessage(message)
            } catch (e: Exception) {
                supervisor.handleFailure(this, e)
            }
        }
    }

    abstract suspend fun handleMessage(message: ActorMessage)
}

// Message types
sealed class ActorMessage {
    data class UserRequest(val content: String) : ActorMessage()
    data class ToolResult(val toolId: String, val result: Any) : ActorMessage()
    data class SystemEvent(val event: SystemEventType) : ActorMessage()
}
```

### Pipeline DSL

```kotlin
// Pipeline DSL in Kotlin
class PipelineBuilder {
    private val blocks = mutableListOf<PipelineBlock>()

    fun block(name: String, config: Map<String, Any> = emptyMap()) {
        blocks.add(PipelineBlock(name, config))
    }

    fun build(): Pipeline = Pipeline(blocks)
}

// Usage
val pipeline = pipeline {
    block("protocol.ollama")
    block("codec.mcp_over_xml")
    block("tools.negotiation_advertise")
    block("family.qwen3")
}
```

### Neutral History with Kotlin

```kotlin
// Neutral History implementation
class NeutralHistoryXml : NeutralHistory {
    private val events = mutableListOf<NeutralEvent>()

    override suspend fun store(event: NeutralEvent) {
        events.add(event)
        // XML serialization would happen here
    }

    override fun retrieve(query: NeutralQuery): Flow<NeutralEvent> = flow {
        events.filter { matchesQuery(it, query) }
            .forEach { emit(it) }
    }
}
```

### End-to-End Data Flow (Kotlin)

- **Ingress**: UI (Compose desktop/web/mobile) or CLI sends a `UserRequest` to `ActorSystem`.
- **Routing**: `ActorSupervisor` delegates to Main/Advisor/Supervisor actors using channel-based mailboxes.
- **Negotiation**: `PipelineEngine` selects blocks and `MCPCore` negotiates the tool format (MCP → functions → XML).
- **Execution**: `MCPExecutor` invokes providers (e.g., Ollama) via provider-specific clients; results stream through Flow.
- **Safety Loop**: `ApprovalSystem` must approve requests; `RiskAssessment` annotates actions before dispatch.
- **Persistence**: `NeutralHistoryXml` records every event (tool calls, responses, mood changes) with lane attribution.
- **Feedback**: `MoodManager` updates emotional state; outputs modulate future prompt shaping.
- **Egress**: Composed response sent back through the initiating actor to UI/CLI.

### Concurrency & Threading Model

- **Coroutines everywhere**: All actors use structured concurrency; long-running work lives on `Dispatchers.IO` or custom dispatchers.
- **Backpressure**: Channel capacities kept small for control planes; heavy payloads stream via Flow to avoid mailbox bloat.
- **Neutral History writes**: Buffered, ordered by timestamp; persistence handled off the main actor dispatcher.
- **SNN compute**: Spiking network runs on a dedicated dispatcher to avoid starving actor control messages.

### Safety Path (Mandatory)

- **Supervisor as gatekeeper**: Every tool call path must traverse Supervisor approval.
- **Risk scoring**: `RiskAssessment` attaches risk metadata; high-risk paths require explicit confirmation.
- **Auditability**: All approvals and denials persisted in Neutral History with provenance.

### Kotlin Multiplatform Integration

- **Common-first**: Business logic in `lib/src/commonMain`; platform shims only wrap IO (HTTP, storage, UI bridges).
- **Providers**: HTTP clients abstracted through expect/actual to support JVM and JS.
- **Storage**: Neutral History storage interface is common; file/DB backends implemented per platform.
- **UI Bridges**: Compose screens consume Flows from actors; no platform-specific business rules in UI.

---


[Back to Kotlin-Aligned Architecture Overview](Kotlin-Aligned-Architecture-Overview)
