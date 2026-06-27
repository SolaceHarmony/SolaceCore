<!-- topic: Reference -->
<!-- title: LangChain Package-by-Package Improvements -->

## Package-by-Package Improvements

### 1. io.github.solaceharmony.core.actor Package

Current:
```kotlin
abstract class Actor {
    protected abstract suspend fun processMessage(message: ActorMessage)
}
```

LangChain-like improvements:
```kotlin
interface Chain<I, O> {
    suspend fun run(input: I): O
}

abstract class ChainableActor<I, O> : Actor(), Chain<I, O> {
    override suspend fun processMessage(message: ActorMessage) {
        val input = message.payload as? I ?: throw IllegalArgumentException()
        val output = run(input)
        sendResult(output)
    }
}

// Usage
class TextProcessingActor : ChainableActor<String, String>() {
    override suspend fun run(input: String): String {
        // Process text
        return processedText
    }
}
```

### 2. io.github.solaceharmony.core.channels Package

Current:
```kotlin
interface Port<T> {
    val id: String
    val type: KClass<T>
}
```

LangChain-like improvements:
```kotlin
interface ChainPort<I, O> : Port<I> {
    suspend fun process(input: I): O
    val nextChain: ChainPort<O, *>?

    suspend fun run(input: I) {
        val output = process(input)
        nextChain?.run(output)
    }
}
```

### 3. io.github.solaceharmony.core.common Package

Add LangChain-like utilities:
```kotlin
object Prompts {
    fun loadPromptTemplate(path: String): PromptTemplate
    fun parsePromptTemplate(text: String): PromptTemplate
}

object Tools {
    fun registerTool(name: String, tool: Tool)
    fun getTool(name: String): Tool?
}

object Memory {
    fun createBufferMemory(): Memory
    fun createVectorMemory(): Memory
}
```



[Back to LangChain Type-Safe Dynamic Wiring](LangChain-Type-Safe-Dynamic-Wiring)
