package ai.solace.prototype.chain

import ai.solace.core.actor.Actor
import ai.solace.prototype.actor.ActorMessage
import ai.solace.prototype.memory.Memory
import ai.solace.prototype.tools.Tool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Enhanced Chain Actor that supports LangChain-like functionality
 */
abstract class ChainActor(
    id: String,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    config: ChainConfig = ChainConfig()
) : Actor(id, scope) {
    
    protected val memory: Memory? = config.memory
    protected val tools: MutableList<Tool> = mutableListOf()
    private val metrics = ChainMetrics()
    private val _state = MutableStateFlow<ChainState>(ChainState.Initialized)
    private val mutex = Mutex()
    
    val state: StateFlow<ChainState> = _state
    
    data class ChainConfig(
        val memory: Memory? = null,
        val maxRetries: Int = 3,
        val timeout: Duration = 30.seconds,
        val errorStrategy: ErrorStrategy = ErrorStrategy.Stop
    )
    
    enum class ErrorStrategy {
        Stop, Continue, Retry
    }
    
    sealed class ChainState {
        object Initialized : ChainState()
        object Running : ChainState()
        object Stopped : ChainState()
        data class Error(val message: String) : ChainState()
    }
    
    protected suspend fun withMemory(block: suspend (Memory) -> Unit) {
        memory?.let { block(it) }
    }
    
    suspend fun registerTool(tool: Tool) = mutex.withLock {
        tools.add(tool)
    }
    
    protected suspend fun executeTool(name: String, input: Map<String, Any>): Tool.ToolResult {
        val tool = tools.find { it.name == name }
        return tool?.execute(input) ?: Tool.ToolResult(
            success = false,
            result = null,
            error = "Tool not found: $name"
        )
    }
    
    override suspend fun processMessage(message: ActorMessage) {
        try {
            metrics.recordMessageReceived()
            when (message) {
                is ActorMessage.LLMRequest -> processLLMRequest(message)
                is ActorMessage.MemoryOperation -> processMemoryOperation(message)
                is ActorMessage.ToolRequest -> processToolRequest(message)
                else -> handleUnknownMessage(message)
            }
            metrics.recordMessageProcessed()
        } catch (e: Exception) {
            handleError(e, message)
            metrics.recordError()
        }
    }
    
    protected abstract suspend fun processLLMRequest(message: ActorMessage.LLMRequest)
    
    protected open suspend fun processMemoryOperation(message: ActorMessage.MemoryOperation) {
        withMemory { memory ->
            when (message.operation) {
                MemoryOp.GET -> memory.get(message.key)
                MemoryOp.SET -> memory.set(message.key, message.value!!)
                MemoryOp.DELETE -> memory.delete(message.key)
                MemoryOp.CLEAR -> memory.clear()
                MemoryOp.GET_CONTEXT -> memory.getContext()
            }
        }
    }
    
    protected open suspend fun processToolRequest(message: ActorMessage.ToolRequest) {
        val result = executeTool(message.toolName, message.input)
        val response = ActorMessage.ToolResponse(
            correlationId = message.correlationId,
            sender = id,
            toolName = message.toolName,
            success = result.success,
            result = result.result,
            error = result.error
        )
        // Send response through appropriate port
    }
    
    protected open suspend fun handleUnknownMessage(message: ActorMessage) {
        throw IllegalArgumentException("Unknown message type: ${message::class.simpleName}")
    }
    
    override fun handleError(error: Exception, message: ActorMessage) {
        _state.value = ChainState.Error(error.message ?: "Unknown error")
        super.handleError(error, message)
    }
}

class ChainMetrics {
    private var messagesReceived: Long = 0
    private var messagesProcessed: Long = 0
    private var errors: Long = 0
    private var tokenUsage: Long = 0
    private var toolCalls: Long = 0
    
    fun recordMessageReceived() { messagesReceived++ }
    fun recordMessageProcessed() { messagesProcessed++ }
    fun recordError() { errors++ }
    fun recordTokenUsage(usage: TokenUsage) { tokenUsage += usage.totalTokens }
    fun recordToolCall() { toolCalls++ }
    
    fun getMetrics(): Map<String, Long> = mapOf(
        "messages_received" to messagesReceived,
        "messages_processed" to messagesProcessed,
        "errors" to errors,
        "token_usage" to tokenUsage,
        "tool_calls" to toolCalls
    )
}