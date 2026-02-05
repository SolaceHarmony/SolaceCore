package ai.solace.prototype.chain

import ai.solace.prototype.actor.ActorMessage
import ai.solace.prototype.actor.TokenUsage
import ai.solace.prototype.memory.Memory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.time.Duration.Companion.seconds

/**
 * Example implementation of an LLM Chain using the enhanced actor system
 */
class LLMChain(
    id: String,
    private val model: LanguageModel,
    private val prompt: PromptTemplate,
    memory: Memory? = null,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : ChainActor(
    id = id,
    scope = scope,
    config = ChainConfig(
        memory = memory,
        timeout = 60.seconds
    )
) {
    
    override suspend fun processLLMRequest(message: ActorMessage.LLMRequest) {
        // Get context from memory if available
        val context = memory?.getContext() ?: emptyMap()
        
        // Format prompt with context
        val enhancedPrompt = prompt.format(
            message.parameters + mapOf(
                "context" to context,
                "input" to message.prompt
            )
        )
        
        // Generate response
        val completion = model.generate(
            prompt = enhancedPrompt,
            parameters = message.parameters
        )
        
        // Store in memory if needed
        memory?.set("last_response", completion.text)
        
        // Create response message
        val response = ActorMessage.LLMResponse(
            correlationId = message.correlationId,
            sender = id,
            completion = completion.text,
            usage = completion.usage,
            metadata = mapOf(
                "model" to message.model,
                "context_length" to context.size
            )
        )
        
        // Update metrics
        metrics.recordTokenUsage(completion.usage)
        
        // Send response through output port
        // getInterface().output("llm_output").send(response)
    }
}

/**
 * Interface for language models
 */
interface LanguageModel {
    suspend fun generate(
        prompt: String,
        parameters: Map<String, Any> = emptyMap()
    ): Completion
    
    data class Completion(
        val text: String,
        val usage: TokenUsage,
        val metadata: Map<String, Any> = emptyMap()
    )
}

/**
 * Interface for prompt templates
 */
interface PromptTemplate {
    val template: String
    val variables: List<String>
    
    fun format(variables: Map<String, Any>): String {
        var result = template
        variables.forEach { (key, value) ->
            result = result.replace("{$key}", value.toString())
        }
        return result
    }
    
    fun validate(variables: Map<String, Any>): Boolean {
        return this.variables.all { variables.containsKey(it) }
    }
}

/**
 * Example implementation of a chat prompt template
 */
class ChatPromptTemplate(
    override val template: String,
    override val variables: List<String>,
    private val systemMessage: String? = null
) : PromptTemplate {
    
    override fun format(variables: Map<String, Any>): String {
        require(validate(variables)) { "Missing required variables" }
        
        val formattedTemplate = super.format(variables)
        return buildString {
            systemMessage?.let {
                appendLine("System: $it")
                appendLine()
            }
            append(formattedTemplate)
        }
    }
}