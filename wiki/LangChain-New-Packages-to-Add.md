<!-- topic: Reference -->
<!-- title: LangChain New Packages to Add -->

## New Packages to Add

### 1. io.github.solaceharmony.core.llm
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

### 2. io.github.solaceharmony.core.memory
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

### 3. io.github.solaceharmony.core.tools
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



[Back to LangChain Type-Safe Dynamic Wiring](LangChain-Type-Safe-Dynamic-Wiring)
