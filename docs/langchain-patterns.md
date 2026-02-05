# LangChain Pattern Implementation in SolaceCore

This guide shows how to implement common LangChain patterns using SolaceCore's actor-based architecture.

## Chain Patterns

### Simple Chain

**LangChain Python:**
```python
from langchain.chains import LLMChain
from langchain.prompts import PromptTemplate

chain = LLMChain(
    llm=OpenAI(),
    prompt=PromptTemplate(
        template="Answer: {question}",
        input_variables=["question"]
    )
)
```

**SolaceCore Kotlin:**
```kotlin
class LLMChain(
    private val llm: LanguageModel,
    private val prompt: PromptTemplate
) : Chain() {
    override suspend fun processMessage(message: ActorMessage) {
        val question = message.payload as String
        val formattedPrompt = prompt.format(mapOf("question" to question))
        val result = llm.generate(formattedPrompt)
        outputPort.send(result)
    }
}
```

### Sequential Chain

**LangChain Python:**
```python
from langchain.chains import SimpleSequentialChain

chain = SimpleSequentialChain(chains=[
    chain1,
    chain2,
    chain3
])
```

**SolaceCore Kotlin:**
```kotlin
class SequentialChain(
    private val chains: List<Chain>
) : Chain() {
    init {
        chains.windowed(2) { (first, second) ->
            actorInterface.connect(
                first.getInterface().output("output"),
                second.getInterface().input("input")
            )
        }
        
        // Connect first and last chains to this chain's ports
        actorInterface.connect(
            inputPort,
            chains.first().getInterface().input("input")
        )
        actorInterface.connect(
            chains.last().getInterface().output("output"),
            outputPort
        )
    }
}
```

## Memory Implementation

### Conversation Memory

**LangChain Python:**
```python
from langchain.memory import ConversationBufferMemory

memory = ConversationBufferMemory()
chain = LLMChain(
    llm=OpenAI(),
    prompt=prompt,
    memory=memory
)
```

**SolaceCore Kotlin:**
```kotlin
interface Memory {
    suspend fun load(): Map<String, Any>
    suspend fun save(state: Map<String, Any>)
}

class ConversationMemory : Memory {
    private val buffer = mutableListOf<String>()
    
    override suspend fun load(): Map<String, Any> {
        return mapOf("history" to buffer.joinToString("\n"))
    }
    
    override suspend fun save(state: Map<String, Any>) {
        state["message"]?.toString()?.let { buffer.add(it) }
    }
}

class MemoryChain(
    private val llm: LanguageModel,
    private val memory: Memory
) : Chain() {
    override suspend fun processMessage(message: ActorMessage) {
        val history = memory.load()
        val response = llm.generate(
            "${history["history"]}\nUser: ${message.payload}"
        )
        memory.save(mapOf("message" to response))
        outputPort.send(response)
    }
}
```

## Tools Integration

### Tool Usage

**LangChain Python:**
```python
from langchain.agents import Tool, initialize_agent
from langchain.llms import OpenAI

tools = [
    Tool(
        name="Search",
        func=search_func,
        description="Search the web"
    )
]

agent = initialize_agent(
    tools, 
    OpenAI(), 
    agent="zero-shot-react-description"
)
```

**SolaceCore Kotlin:**
```kotlin
interface Tool {
    val name: String
    val description: String
    suspend fun execute(input: String): String
}

class SearchTool : Tool {
    override val name = "Search"
    override val description = "Search the web"
    
    override suspend fun execute(input: String): String {
        // Implementation
    }
}

class AgentChain(
    private val llm: LanguageModel,
    private val tools: List<Tool>
) : Chain() {
    override suspend fun processMessage(message: ActorMessage) {
        // Tool selection and execution logic
    }
}
```

## Callbacks and Observability

### Logging Callbacks

**LangChain Python:**
```python
from langchain.callbacks import StdOutCallbackHandler

handler = StdOutCallbackHandler()
chain = LLMChain(callbacks=[handler])
```

**SolaceCore Kotlin:**
```kotlin
class LoggingPort : OutputPort<String>("logging", String::class) {
    override suspend fun send(message: String) {
        println(message)
        super.send(message)
    }
}

class ObservableChain : Chain() {
    private val loggingPort = LoggingPort()
    
    init {
        actorInterface.apply {
            // Connect logging port
        }
    }
}
```

## Prompt Management

### Prompt Templates

**LangChain Python:**
```python
from langchain.prompts import PromptTemplate

prompt = PromptTemplate(
    template="Answer the question: {question}",
    input_variables=["question"]
)
```

**SolaceCore Kotlin:**
```kotlin
class PromptTemplate(
    private val template: String,
    private val variables: List<String>
) {
    fun format(values: Map<String, String>): String {
        var result = template
        values.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }
        return result
    }
}
```

## Document Processing

### Document Loading

**LangChain Python:**
```python
from langchain.document_loaders import TextLoader
from langchain.text_splitter import CharacterTextSplitter

loader = TextLoader("document.txt")
text_splitter = CharacterTextSplitter()
docs = loader.load_and_split()
```

**SolaceCore Kotlin:**
```kotlin
interface DocumentLoader {
    suspend fun load(path: String): Document
}

class TextLoader : DocumentLoader {
    override suspend fun load(path: String): Document {
        // Implementation
    }
}

class DocumentProcessingChain(
    private val loader: DocumentLoader,
    private val splitter: TextSplitter
) : Chain() {
    override suspend fun processMessage(message: ActorMessage) {
        val path = message.payload as String
        val doc = loader.load(path)
        val chunks = splitter.split(doc)
        outputPort.send(chunks)
    }
}
```

## Vector Store Integration

### Vector Storage

**LangChain Python:**
```python
from langchain.vectorstores import Chroma

vectorstore = Chroma.from_documents(
    documents=docs,
    embedding=embeddings
)
```

**SolaceCore Kotlin:**
```kotlin
interface VectorStore {
    suspend fun store(documents: List<Document>, embeddings: List<FloatArray>)
    suspend fun search(query: FloatArray, k: Int): List<Document>
}

class VectorStoreChain(
    private val store: VectorStore,
    private val embedder: Embedder
) : Chain() {
    override suspend fun processMessage(message: ActorMessage) {
        when (message.type) {
            "store" -> {
                val docs = message.payload as List<Document>
                val embeddings = docs.map { embedder.embed(it.text) }
                store.store(docs, embeddings)
            }
            "search" -> {
                val query = message.payload as String
                val queryEmbedding = embedder.embed(query)
                val results = store.search(queryEmbedding, 5)
                outputPort.send(results)
            }
        }
    }
}
```

## Best Practices

1. **Message Typing**
   - Use sealed classes for message types
   - Include correlation IDs
   - Handle message versioning

2. **Error Handling**
   - Implement proper error ports
   - Use supervision strategies
   - Maintain message context in errors

3. **Resource Management**
   - Properly dispose of resources
   - Implement cleanup in stop()
   - Handle connection lifecycle

4. **Testing**
   - Use TestActorInterface for testing
   - Mock external services
   - Test message flows