# Making SolaceCore More LangChain-Like with Focus on Type-Safe Dynamic Wiring

## Type-Safe Dynamic Wiring System

### 1. Port Type System
```kotlin
// Base port type that enforces type constraints
sealed interface PortType<in I, out O> {
    fun accepts(input: KClass<*>): Boolean
    fun produces(): KClass<O>
}

// Example port types
object TextToText : PortType<String, String> {
    override fun accepts(input: KClass<*>) = input == String::class
    override fun produces() = String::class
}

object TextToVector : PortType<String, List<Double>> {
    override fun accepts(input: KClass<*>) = input == String::class
    override fun produces() = List::class
}
```

### 2. Enhanced Port System
```kotlin
interface Port<I : Any, O : Any> {
    val id: String
    val name: String
    val portType: PortType<I, O>
    
    // Runtime type checking
    fun canAccept(input: Any): Boolean = 
        portType.accepts(input::class)
    
    fun canConnectTo(other: Port<*, *>): Boolean =
        other.portType.accepts(portType.produces())
}

class InputPort<I : Any, O : Any>(
    override val name: String,
    override val portType: PortType<I, O>
) : Port<I, O> {
    private val channel = Channel<I>(Channel.BUFFERED)
    
    suspend fun receive(): I = channel.receive()
    
    suspend fun process(input: I): O {
        require(canAccept(input)) { "Invalid input type" }
        // Process input according to port type
        return processInput(input)
    }
}

class OutputPort<I : Any, O : Any>(
    override val name: String,
    override val portType: PortType<I, O>
) : Port<I, O> {
    private val connections = mutableListOf<InputPort<O, *>>()
    
    suspend fun connect(input: InputPort<O, *>) {
        require(canConnectTo(input)) { "Incompatible port types" }
        connections.add(input)
    }
    
    suspend fun send(value: O) {
        connections.forEach { it.receive(value) }
    }
}
```

### 3. Dynamic Connection Management
```kotlin
class ConnectionManager {
    private val connections = mutableMapOf<String, Connection>()
    
    suspend fun connect(
        source: OutputPort<*, *>,
        target: InputPort<*, *>
    ): Connection {
        require(source.canConnectTo(target)) {
            "Cannot connect ${source.portType} to ${target.portType}"
        }
        
        return Connection(source, target).also {
            connections[it.id] = it
        }
    }
    
    suspend fun disconnect(connectionId: String) {
        connections.remove(connectionId)?.dispose()
    }
    
    // Dynamic reconnection during runtime
    suspend fun rewire(
        connection: Connection,
        newTarget: InputPort<*, *>
    ) {
        require(connection.source.canConnectTo(newTarget)) {
            "Cannot rewire to incompatible port"
        }
        
        connection.dispose()
        connect(connection.source, newTarget)
    }
}
```

### 1. Chain-of-Responsibility Pattern
Current SolaceCore uses Actor model primarily. While powerful, we should add LangChain-like chaining:

```kotlin
// Current Actor approach
class ProcessingActor : Actor() {
    override suspend fun processMessage(message: ActorMessage) {
        // Process in one place
    }
}

// LangChain-like approach
interface Chain<I, O> {
    suspend fun run(input: I): O
}

class TextProcessingChain : Chain<String, String> {
    private val steps = mutableListOf<Chain<String, String>>()
    
    fun addStep(step: Chain<String, String>) {
        steps.add(step)
    }
    
    override suspend fun run(input: String): String {
        return steps.fold(input) { acc, step -> step.run(acc) }
    }
}
```

### 2. Prompt Templates
Current system lacks structured prompt management. Add LangChain-like templates:

```kotlin
class PromptTemplate(
    private val template: String,
    private val inputVariables: List<String>
) {
    fun format(variables: Map<String, String>): String {
        var result = template
        variables.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }
        return result
    }
}

// Usage
val template = PromptTemplate(
    template = "Hello {name}, how can I help with {topic}?",
    inputVariables = listOf("name", "topic")
)
```

### 3. Memory Systems
Current Actor state is basic. Add LangChain-like memory:

```kotlin
interface Memory {
    suspend fun readMemories(keys: List<String>): Map<String, Any>
    suspend fun saveMemories(memories: Map<String, Any>)
}

class ConversationBufferMemory : Memory {
    private val messages = mutableListOf<Message>()
    
    override suspend fun readMemories(keys: List<String>): Map<String, Any> {
        return mapOf(
            "history" to messages.joinToString("\n") { it.toString() }
        )
    }
    
    override suspend fun saveMemories(memories: Map<String, Any>) {
        memories["input"]?.let { messages.add(Message.Human(it.toString())) }
        memories["output"]?.let { messages.add(Message.AI(it.toString())) }
    }
}
```

## Package-by-Package Improvements

### 1. ai.solace.core.actor Package

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

### 2. ai.solace.core.channels Package

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

### 3. ai.solace.core.common Package

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

## New Packages to Add

### 1. ai.solace.core.llm
LangChain-like LLM abstractions:
```kotlin
interface LLM {
    suspend fun complete(prompt: String): String
    suspend fun chat(messages: List<Message>): Message
}

class OpenAILLM(private val apiKey: String) : LLM {
    override suspend fun complete(prompt: String): String {
        // OpenAI completion implementation
    }
}
```

### 2. ai.solace.core.memory
Enhanced memory systems:
```kotlin
interface VectorStore {
    suspend fun addDocuments(documents: List<Document>)
    suspend fun similaritySearch(query: String): List<Document>
}

class ChromaVectorStore : VectorStore {
    // Implementation
}
```

### 3. ai.solace.core.tools
Tool abstractions like LangChain:
```kotlin
interface Tool {
    val name: String
    val description: String
    suspend fun run(input: String): String
}

class WebSearchTool : Tool {
    override val name = "web_search"
    override val description = "Search the web for information"
    
    override suspend fun run(input: String): String {
        // Implementation
    }
}
```

## Implementation Priorities

1. **Immediate (Week 1-2)**
   - Add Chain interface and basic chaining functionality
   - Implement PromptTemplate system
   - Basic Memory interface

2. **Short-term (Week 3-4)**
   - Convert existing Actors to support chaining
   - Implement basic LLM abstractions
   - Add tool system

3. **Medium-term (Month 2)**
   - Vector store integration
   - Advanced memory systems
   - Enhanced prompt management

## Migration Strategy

1. **Phase 1: Chain Support**
   - Add Chain interface
   - Make Actors chain-compatible
   - Keep backward compatibility

2. **Phase 2: New Features**
   - Add prompt templates
   - Implement memory systems
   - Add tool abstractions

3. **Phase 3: Full Integration**
   - Merge Actor and Chain patterns
   - Add advanced features
   - Update documentation

## Key Differences to Consider

1. **Actor Model vs Chain Pattern**
   - Keep Actor model for concurrency
   - Add Chain pattern for processing
   - Blend both patterns where appropriate

2. **State Management**
   - Actors: Mutable state
   - LangChain: Immutable chains
   - Solution: Support both patterns

3. **Processing Flow**
   - Actors: Message-based
   - LangChain: Chain-based
   - Solution: Chain messages through actors

## Notes

- Keep existing Actor functionality for concurrency
- Add LangChain patterns for AI workflow
- Maintain backward compatibility
- Focus on developer experience

Please update this document as the implementation progresses.