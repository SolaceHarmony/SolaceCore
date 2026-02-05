package ai.solace.prototype.tools

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass

/**
 * Interface for external tools that can be used by chains
 */
interface Tool {
    val name: String
    val description: String
    val parameters: List<ToolParameter>
    
    suspend fun execute(input: Map<String, Any>): ToolResult
    
    data class ToolParameter(
        val name: String,
        val type: KClass<*>,
        val description: String,
        val required: Boolean = true,
        val defaultValue: Any? = null
    )
    
    data class ToolResult(
        val success: Boolean,
        val result: Any?,
        val error: String? = null,
        val metadata: Map<String, Any> = emptyMap()
    )
}

/**
 * Manager for registering and executing tools
 */
class ToolManager {
    private val mutex = Mutex()
    private val tools = mutableMapOf<String, Tool>()
    
    suspend fun register(tool: Tool) = mutex.withLock {
        tools[tool.name] = tool
    }
    
    suspend fun unregister(name: String) = mutex.withLock {
        tools.remove(name)
    }
    
    suspend fun execute(name: String, input: Map<String, Any>): Tool.ToolResult {
        val tool = tools[name] ?: return Tool.ToolResult(
            success = false,
            result = null,
            error = "Tool not found: $name"
        )
        
        // Validate input parameters
        val missingParams = tool.parameters
            .filter { it.required }
            .map { it.name }
            .filter { !input.containsKey(it) }
        
        if (missingParams.isNotEmpty()) {
            return Tool.ToolResult(
                success = false,
                result = null,
                error = "Missing required parameters: $missingParams"
            )
        }
        
        return try {
            tool.execute(input)
        } catch (e: Exception) {
            Tool.ToolResult(
                success = false,
                result = null,
                error = "Tool execution failed: ${e.message}"
            )
        }
    }
    
    fun getAvailableTools(): List<Tool> = tools.values.toList()
}

/**
 * Example search tool implementation
 */
class SearchTool : Tool {
    override val name = "search"
    override val description = "Search for information on the internet"
    override val parameters = listOf(
        Tool.ToolParameter(
            name = "query",
            type = String::class,
            description = "The search query"
        ),
        Tool.ToolParameter(
            name = "limit",
            type = Int::class,
            description = "Maximum number of results",
            required = false,
            defaultValue = 5
        )
    )
    
    override suspend fun execute(input: Map<String, Any>): Tool.ToolResult {
        val query = input["query"] as String
        val limit = (input["limit"] as? Int) ?: 5
        
        return try {
            // Implement actual search logic here
            val results = listOf("Result 1", "Result 2") // Dummy results
            Tool.ToolResult(
                success = true,
                result = results,
                metadata = mapOf(
                    "query" to query,
                    "limit" to limit
                )
            )
        } catch (e: Exception) {
            Tool.ToolResult(
                success = false,
                result = null,
                error = "Search failed: ${e.message}"
            )
        }
    }
}

/**
 * Example calculator tool implementation
 */
class CalculatorTool : Tool {
    override val name = "calculator"
    override val description = "Perform mathematical calculations"
    override val parameters = listOf(
        Tool.ToolParameter(
            name = "expression",
            type = String::class,
            description = "Mathematical expression to evaluate"
        )
    )
    
    override suspend fun execute(input: Map<String, Any>): Tool.ToolResult {
        val expression = input["expression"] as String
        
        return try {
            // Simple expression evaluator - replace with proper implementation
            val result = evaluateExpression(expression)
            Tool.ToolResult(
                success = true,
                result = result,
                metadata = mapOf("expression" to expression)
            )
        } catch (e: Exception) {
            Tool.ToolResult(
                success = false,
                result = null,
                error = "Calculation failed: ${e.message}"
            )
        }
    }
    
    private fun evaluateExpression(expression: String): Double {
        // Implement proper expression evaluation
        return 0.0 // Dummy implementation
    }
}