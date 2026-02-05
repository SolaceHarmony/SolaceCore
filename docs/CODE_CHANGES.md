# Required Code Changes for LangChain Integration

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

## 5. Required Interface Changes

### 5.1 Add Common Chain Interface
```kotlin
interface Chain {
    val memory: Memory?
    val metrics: ChainMetrics
    val tools: List<Tool>
    
    suspend fun execute(input: Any): Any
    suspend fun reset()
}
```

### 5.2 Add Chain Actor Base Class
```kotlin
abstract class ChainActor(
    id: String = Uuid.random().toString(),
    config: ChainConfig
) : Actor(id), Chain {
    override val memory: Memory? = config.memory
    override val metrics = ChainMetrics()
    override val tools = mutableListOf<Tool>()
    
    protected suspend fun withMemory(block: suspend (Memory) -> Unit) {
        memory?.let { block(it) }
    }
}
```

## 6. Directory Structure Changes

Add new directories:
```
lib/
├── src/
│   ├── commonMain/
│   │   └── kotlin/
│   │       └── ai/
│   │           └── solace/
│   │               └── core/
│   │                   ├── actor/        (existing)
│   │                   ├── channels/     (existing)
│   │                   ├── common/       (existing)
│   │                   ├── chain/        (new)
│   │                   │   ├── base/
│   │                   │   ├── llm/
│   │                   │   ├── memory/
│   │                   │   └── tools/
│   │                   ├── metrics/      (new)
│   │                   └── tools/        (new)
```

## 7. Testing Changes

Add new test files:
```kotlin
// ChainActorTest.kt
class ChainActorTest {
    @Test
    fun `test memory operations`() {
        // Test memory integration
    }
    
    @Test
    fun `test tool execution`() {
        // Test tool execution
    }
}

// ToolTest.kt
class ToolTest {
    @Test
    fun `test tool registration and execution`() {
        // Test tool management
    }
}

// MemoryTest.kt
class MemoryTest {
    @Test
    fun `test conversation memory`() {
        // Test memory operations
    }
}
```

## Implementation Priority

1. **High Priority (Phase 1)**
   - Implement typed ActorMessage hierarchy
   - Add Memory interface and basic implementation
   - Add Chain interface and base ChainActor
   - Update ActorInterface with memory support

2. **Medium Priority (Phase 2)**
   - Implement Tool system
   - Add specialized ports
   - Enhance metrics
   - Add configuration support

3. **Low Priority (Phase 3)**
   - Add advanced memory implementations
   - Implement tracing
   - Add testing utilities
   - Enhance documentation

## Migration Steps

1. **Preparation**
   - Create new package structure
   - Add new interfaces
   - Create test scaffolding

2. **Core Changes**
   - Update Actor class
   - Modify ActorInterface
   - Enhance Port system
   - Add base Chain implementation

3. **Feature Addition**
   - Implement Memory system
   - Add Tool support
   - Enhance metrics
   - Add configuration

4. **Testing & Documentation**
   - Add unit tests
   - Update documentation
   - Create examples
   - Add migration guides

## Impact Analysis

1. **Breaking Changes**
   - ActorMessage structure
   - Port interface
   - Actor constructor

2. **Compatible Changes**
   - New interfaces
   - Additional features
   - Enhanced metrics

3. **Performance Impact**
   - Minor overhead from type checking
   - Additional memory usage for metrics
   - Negligible impact from tool management

## Next Steps

1. Review and prioritize changes
2. Create detailed implementation plan
3. Start with high-priority changes
4. Add tests for new functionality
5. Update documentation