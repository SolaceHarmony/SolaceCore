<!-- topic: Reference -->
<!-- title: LangChain ActorInterface Code Changes -->

## 2. ActorInterface.kt Changes

Current Implementation:
```kotlin
class ActorInterface(private val scope: CoroutineScope) : Lifecycle {
    private val connectionManager = ConnectionManager(scope)
    private val ports = mutableMapOf<String, Port<*>>()
}
```

Issues:
1. No support for memory management
2. Limited port type system
3. No built-in tool support
4. Basic connection management

Recommended Changes:
```kotlin
class ActorInterface(
    private val scope: CoroutineScope,
    private val config: InterfaceConfig = InterfaceConfig()
) : Lifecycle {
    // Add memory management
    private val memory: Memory? = null
    private val toolManager = ToolManager()

    data class InterfaceConfig(
        val memoryConfig: MemoryConfig? = null,
        val portConfigs: Map<String, PortConfig> = emptyMap(),
        val toolConfigs: List<ToolConfig> = emptyList()
    )

    // Add specialized port creation methods
    fun <T : Any> llmInput(name: String): InputPort<LLMRequest> =
        input(name, LLMRequest::class)

    fun <T : Any> llmOutput(name: String): OutputPort<LLMResponse> =
        output(name, LLMResponse::class)

    // Add tool support
    suspend fun registerTool(tool: Tool) = toolManager.register(tool)
    suspend fun executeTool(name: String, input: Map<String, Any>) =
        toolManager.execute(name, input)
}
```



[Back to LangChain Code Changes](LangChain-Code-Changes)
