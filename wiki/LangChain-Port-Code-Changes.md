<!-- topic: Reference -->
<!-- title: LangChain Port Code Changes -->

## 3. Port.kt Changes

Current Implementation:
```kotlin
interface Port<T : Any> : Disposable {
    val id: String
    val name: String
    val type: KClass<T>
}
```

Issues:
1. Basic port interface lacks specialized functionality
2. No support for different message patterns
3. Limited type constraints

Recommended Changes:
```kotlin
interface Port<T : Any> : Disposable {
    val id: String
    val name: String
    val type: KClass<T>
    val config: PortConfig

    data class PortConfig(
        val bufferSize: Int = Channel.BUFFERED,
        val timeoutMs: Long = 5000,
        val retry: RetryConfig? = null
    )
}

// Add specialized ports
interface PromptPort : Port<String> {
    suspend fun formatPrompt(template: String, variables: Map<String, String>): String
}

interface MemoryPort : Port<MemoryOperation> {
    suspend fun recall(key: String): Any?
    suspend fun memorize(key: String, value: Any)
}

interface ToolPort : Port<ToolRequest> {
    suspend fun execute(request: ToolRequest): ToolResponse
}
```



[Back to LangChain Code Changes](LangChain-Code-Changes)
