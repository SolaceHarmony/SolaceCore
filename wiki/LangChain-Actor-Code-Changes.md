<!-- topic: Reference -->
<!-- title: LangChain Actor Code Changes -->

## 1. Actor.kt Changes

Current Implementation:
```kotlin
data class ActorMessage(
    val correlationId: String = Uuid.random().toString(),
    val type: String,
    val payload: Any,
    val sender: String? = null
)
```

Issues:
1. Generic `payload: Any` lacks type safety
2. Simple `type: String` doesn't enforce valid message types
3. No built-in support for memory or chain-specific features

Recommended Changes:
```kotlin
// Add sealed class hierarchy for messages
sealed class ActorMessage {
    abstract val correlationId: String
    abstract val sender: String?

    data class LLMRequest(
        override val correlationId: String = Uuid.random().toString(),
        override val sender: String? = null,
        val prompt: String,
        val params: Map<String, Any> = emptyMap()
    ) : ActorMessage()

    data class LLMResponse(
        override val correlationId: String,
        override val sender: String? = null,
        val completion: String,
        val usage: TokenUsage
    ) : ActorMessage()

    // Add other message types
}

// Add configuration support to Actor class
abstract class Actor(
    val id: String = Uuid.random().toString(),
    protected val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    protected val config: ActorConfig = ActorConfig()
) {
    // Add configuration data class
    data class ActorConfig(
        val bufferSize: Int = Channel.BUFFERED,
        val errorStrategy: ErrorStrategy = ErrorStrategy.STOP,
        val metricsEnabled: Boolean = true
    )

    // Add error handling strategies
    enum class ErrorStrategy {
        STOP, CONTINUE, RETRY
    }
}
```



[Back to LangChain Code Changes](LangChain-Code-Changes)
