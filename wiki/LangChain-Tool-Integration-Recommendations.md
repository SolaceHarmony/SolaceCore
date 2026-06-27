<!-- topic: Reference -->
<!-- title: LangChain Tool Integration Recommendations -->

## 4. Tool Integration

### 4.1 Add Tool Interface

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

    data class ToolResult(
        val success: Boolean,
        val result: Any?,
        val error: String? = null
    )
}
```

### 4.2 Add Tool Manager

```kotlin
class ToolManager {
    private val tools = mutableMapOf<String, Tool>()
    private val mutex = Mutex()

    suspend fun register(tool: Tool) = mutex.withLock {
        tools[tool.name] = tool
    }

    suspend fun execute(name: String, input: Map<String, Any>): Tool.ToolResult {
        return tools[name]?.execute(input)
            ?: Tool.ToolResult(false, null, "Tool not found: $name")
    }
}
```



[Back to LangChain Recommendations](LangChain-Recommendations)
