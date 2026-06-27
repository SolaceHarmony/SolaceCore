<!-- topic: Reference -->
<!-- title: LangChain Metrics and Observability Recommendations -->

## 6. Metrics and Observability

### 6.1 Enhanced Metrics

```kotlin
class ChainMetrics : ActorMetrics() {
    private val tokenUsage = AtomicLong(0)
    private val promptTokens = AtomicLong(0)
    private val completionTokens = AtomicLong(0)
    private val toolCalls = AtomicLong(0)
    private val memoryOperations = AtomicLong(0)

    fun recordTokenUsage(prompt: Long, completion: Long) {
        tokenUsage.addAndGet(prompt + completion)
        promptTokens.addAndGet(prompt)
        completionTokens.addAndGet(completion)
    }

    fun recordToolCall() = toolCalls.incrementAndGet()
    fun recordMemoryOperation() = memoryOperations.incrementAndGet()
}
```

### 6.2 Add Tracing

```kotlin
interface ChainTracer {
    suspend fun startSpan(name: String, attributes: Map<String, String> = emptyMap())
    suspend fun endSpan()
    suspend fun addEvent(name: String, attributes: Map<String, String> = emptyMap())
}

class OpenTelemetryTracer : ChainTracer {
    // Implementation using OpenTelemetry
}
```



[Back to LangChain Recommendations](LangChain-Recommendations)
