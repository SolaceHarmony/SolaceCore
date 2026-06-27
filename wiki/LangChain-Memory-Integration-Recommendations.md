<!-- topic: Reference -->
<!-- title: LangChain Memory Integration Recommendations -->

## 2. Memory System Integration

### 2.1 Add Memory Interface

```kotlin
interface Memory {
    suspend fun get(key: String): Any?
    suspend fun set(key: String, value: Any)
    suspend fun clear()
    suspend fun getContext(): Map<String, Any>

    interface Factory {
        fun create(): Memory
    }
}

class ConversationBufferMemory : Memory {
    private val buffer = mutableListOf<ChatMessage>()
    private val mutex = Mutex()

    override suspend fun getContext(): Map<String, Any> = mutex.withLock {
        mapOf("history" to buffer.joinToString("\n"))
    }
}
```

### 2.2 Add Memory Management to ActorInterface

```kotlin
class ActorInterface(private val scope: CoroutineScope) : Lifecycle {
    private val memory: Memory? = null

    fun setMemory(memory: Memory) {
        this.memory = memory
    }

    suspend fun <T> withMemory(block: suspend (Memory) -> T): T? {
        return memory?.let { block(it) }
    }
}
```



[Back to LangChain Recommendations](LangChain-Recommendations)
