<!-- topic: Reference -->
<!-- title: LangChain New Files Needed -->

## 4. New Files Needed

### 4.1 Memory.kt
```kotlin
interface Memory {
    suspend fun get(key: String): Any?
    suspend fun set(key: String, value: Any)
    suspend fun clear()
    suspend fun getContext(): Map<String, Any>
}

class ConversationMemory : Memory {
    private val buffer = mutableListOf<ChatMessage>()
    private val mutex = Mutex()

    override suspend fun getContext(): Map<String, Any> = mutex.withLock {
        mapOf("history" to buffer.joinToString("\n"))
    }

    // Implement other methods
}
```

### 4.2 Tool.kt
```kotlin
interface Tool {
    val name: String
    val description: String
    val parameters: List<ToolParameter>

    suspend fun execute(input: Map<String, Any>): ToolResult

    data class ToolParameter(
        val name: String,
        val type: KClass<*>,
        val description: String,
        val required: Boolean = true
    )
}

class ToolManager {
    private val tools = mutableMapOf<String, Tool>()
    private val mutex = Mutex()

    suspend fun register(tool: Tool) = mutex.withLock {
        tools[tool.name] = tool
    }

    suspend fun execute(name: String, input: Map<String, Any>): ToolResult =
        tools[name]?.execute(input) ?: ToolResult(false, null, "Tool not found")
}
```

### 4.3 Metrics.kt
```kotlin
class ChainMetrics : ActorMetrics() {
    private val tokenUsage = AtomicLong(0)
    private val promptTokens = AtomicLong(0)
    private val completionTokens = AtomicLong(0)
    private val toolCalls = AtomicLong(0)
    private val memoryOperations = AtomicLong(0)

    // Add specific metric tracking methods
}
```



[Back to LangChain Code Changes](LangChain-Code-Changes)
