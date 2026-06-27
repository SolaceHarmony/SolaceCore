<!-- topic: Reference -->
<!-- title: LangChain Required Interface Changes -->

## 5. Required Interface Changes

### 5.1 Add Common Chain Interface
```kotlin
interface Chain {
    val memory: Memory?
    val metrics: ChainMetrics
    val tools: List<Tool>

    suspend fun execute(input: Any): Any
    suspend fun reset()
}
```

### 5.2 Add Chain Actor Base Class
```kotlin
abstract class ChainActor(
    id: String = Uuid.random().toString(),
    config: ChainConfig
) : Actor(id), Chain {
    override val memory: Memory? = config.memory
    override val metrics = ChainMetrics()
    override val tools = mutableListOf<Tool>()

    protected suspend fun withMemory(block: suspend (Memory) -> Unit) {
        memory?.let { block(it) }
    }
}
```



[Back to LangChain Code Changes](LangChain-Code-Changes)
