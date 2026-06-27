<!-- topic: Reference -->
<!-- title: LangChain Port System Recommendations -->

## 3. Enhanced Port System

### 3.1 Add Specialized Ports for LLM Operations

```kotlin
sealed class ChainPort<T : Any> : Port<T> {
    class LLMInput : ChainPort<String>()
    class LLMOutput : ChainPort<String>()
    class MemoryPort : ChainPort<MemoryOperation>()
    class ToolPort : ChainPort<ToolRequest>()
}

interface PromptPort : Port<String> {
    suspend fun formatPrompt(template: String, variables: Map<String, String>): String
}
```

### 3.2 Add Port Templates

```kotlin
object PortTemplates {
    fun createLLMPorts(actor: ChainActor): Pair<InputPort<String>, OutputPort<String>> {
        return actor.getInterface().let {
            Pair(
                it.input("llm_input", String::class),
                it.output("llm_output", String::class)
            )
        }
    }

    fun createMemoryPorts(actor: ChainActor): Pair<InputPort<MemoryOperation>, OutputPort<MemoryOperation>> {
        // Similar implementation
    }
}
```



[Back to LangChain Recommendations](LangChain-Recommendations)
