package ai.solace.prototype.examples

import ai.solace.prototype.actor.ActorMessage
import ai.solace.prototype.actor.TokenUsage
import ai.solace.prototype.chain.ChatPromptTemplate
import ai.solace.prototype.chain.LLMChain
import ai.solace.prototype.chain.LanguageModel
import ai.solace.prototype.memory.ConversationMemory
import ai.solace.prototype.tools.CalculatorTool
import ai.solace.prototype.tools.SearchTool
import kotlinx.coroutines.runBlocking

/**
 * Example showing how to use the enhanced chain system
 */
fun main() = runBlocking {
    // Create components
    val memory = ConversationMemory(maxHistory = 10)
    val tools = listOf(SearchTool(), CalculatorTool())
    
    // Create a simple LLM implementation
    val model = object : LanguageModel {
        override suspend fun generate(
            prompt: String,
            parameters: Map<String, Any>
        ): LanguageModel.Completion {
            // Simulate LLM response
            return LanguageModel.Completion(
                text = "This is a simulated response to: $prompt",
                usage = TokenUsage(
                    promptTokens = prompt.length,
                    completionTokens = 20,
                    totalTokens = prompt.length + 20
                )
            )
        }
    }
    
    // Create prompt template
    val prompt = ChatPromptTemplate(
        template = """
            Context: {context}
            User: {input}
            Assistant: Let me help you with that.
        """.trimIndent(),
        variables = listOf("context", "input"),
        systemMessage = "You are a helpful AI assistant."
    )
    
    // Create chain
    val chain = LLMChain(
        id = "example-chain",
        model = model,
        prompt = prompt,
        memory = memory
    )
    
    // Register tools
    tools.forEach { chain.registerTool(it) }
    
    // Start chain
    chain.start()
    
    // Create a request
    val request = ActorMessage.LLMRequest(
        prompt = "What's the weather like?",
        model = "gpt-4",
        parameters = mapOf(
            "temperature" to 0.7,
            "max_tokens" to 100
        )
    )
    
    // Send request to chain
    chain.send(request)
    
    // Use tool
    val toolRequest = ActorMessage.ToolRequest(
        toolName = "calculator",
        input = mapOf(
            "expression" to "2 + 2"
        )
    )
    
    chain.send(toolRequest)
    
    // Clean up
    chain.stop()
}

/**
 * Example of creating a sequential chain
 */
class ExampleSequentialChain {
    fun createChain() {
        // Create components
        val memory = ConversationMemory()
        val tools = listOf(SearchTool(), CalculatorTool())
        
        // Create prompt templates
        val searchPrompt = ChatPromptTemplate(
            template = "Search for: {query}",
            variables = listOf("query")
        )
        
        val analysisPrompt = ChatPromptTemplate(
            template = """
                Search results: {search_results}
                Analyze the following: {input}
            """.trimIndent(),
            variables = listOf("search_results", "input")
        )
        
        // Create individual chains
        val searchChain = LLMChain(
            id = "search-chain",
            model = MockLanguageModel(),
            prompt = searchPrompt
        )
        
        val analysisChain = LLMChain(
            id = "analysis-chain",
            model = MockLanguageModel(),
            prompt = analysisPrompt,
            memory = memory
        )
        
        // In a real implementation, these would be connected using the actor interface
        // and message passing between ports
    }
}

class MockLanguageModel : LanguageModel {
    override suspend fun generate(
        prompt: String,
        parameters: Map<String, Any>
    ): LanguageModel.Completion {
        return LanguageModel.Completion(
            text = "Mock response",
            usage = TokenUsage(10, 10, 20)
        )
    }
}