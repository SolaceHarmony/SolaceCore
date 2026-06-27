<!-- topic: Reference -->
<!-- title: LangChain Core Architecture Recommendations -->

## 1. Core Architecture Enhancements

### 1.1 Specialized Message Types

Current:
```kotlin
data class ActorMessage(
    val correlationId: String = Uuid.random().toString(),
    val type: String,
    val payload: Any,
    val sender: String? = null
)
```

Recommended:
```kotlin
sealed class ChainMessage {
    abstract val correlationId: String

    data class LLMRequest(
        override val correlationId: String = Uuid.random().toString(),
        val prompt: String,
        val parameters: Map<String, Any> = emptyMap()
    ) : ChainMessage()

    data class LLMResponse(
        override val correlationId: String,
        val completion: String,
        val tokens: Int,
        val metadata: Map<String, Any> = emptyMap()
    ) : ChainMessage()

    data class MemoryOperation(
        override val correlationId: String,
        val operation: String,
        val key: String,
        val value: Any? = null
    ) : ChainMessage()
}
```

### 1.2 Add Chain-Specific Actor Base Class

```kotlin
abstract class ChainActor(
    id: String = Uuid.random().toString(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : Actor(id, scope) {
    protected val memory: Memory? = null
    protected val callbacks = mutableListOf<ChainCallback>()

    protected suspend fun withMemory(block: suspend (Memory) -> Unit) {
        memory?.let { block(it) }
    }

    protected suspend fun notifyCallbacks(event: ChainEvent) {
        callbacks.forEach { it.onEvent(event) }
    }
}
```



[Back to LangChain Recommendations](LangChain-Recommendations)
