package ai.solace.prototype.actor

import kotlin.random.Random
import kotlin.time.Duration

/**
 * Enhanced message system for LangChain-like functionality
 */
sealed class ActorMessage {
    abstract val correlationId: String
    abstract val sender: String?
    abstract val timestamp: Long
    
    data class LLMRequest(
        override val correlationId: String = generateId(),
        override val sender: String? = null,
        override val timestamp: Long = System.currentTimeMillis(),
        val prompt: String,
        val model: String,
        val parameters: Map<String, Any> = emptyMap(),
        val contextId: String? = null
    ) : ActorMessage()
    
    data class LLMResponse(
        override val correlationId: String,
        override val sender: String? = null,
        override val timestamp: Long = System.currentTimeMillis(),
        val completion: String,
        val usage: TokenUsage,
        val metadata: Map<String, Any> = emptyMap()
    ) : ActorMessage()
    
    data class MemoryOperation(
        override val correlationId: String = generateId(),
        override val sender: String? = null,
        override val timestamp: Long = System.currentTimeMillis(),
        val operation: MemoryOp,
        val key: String,
        val value: Any? = null,
        val context: Map<String, Any> = emptyMap()
    ) : ActorMessage()
    
    data class ToolRequest(
        override val correlationId: String = generateId(),
        override val sender: String? = null,
        override val timestamp: Long = System.currentTimeMillis(),
        val toolName: String,
        val input: Map<String, Any>,
        val timeout: Duration? = null
    ) : ActorMessage()
    
    data class ToolResponse(
        override val correlationId: String,
        override val sender: String? = null,
        override val timestamp: Long = System.currentTimeMillis(),
        val toolName: String,
        val success: Boolean,
        val result: Any?,
        val error: String? = null
    ) : ActorMessage()
    
    companion object {
        private fun generateId(): String = buildString {
            append("msg-")
            append(Random.nextBytes(8).joinToString("") { "%02x".format(it) })
        }
    }
}

enum class MemoryOp {
    GET, SET, DELETE, CLEAR, GET_CONTEXT
}

data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)