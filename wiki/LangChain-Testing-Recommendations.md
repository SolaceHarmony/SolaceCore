<!-- topic: Reference -->
<!-- title: LangChain Testing Recommendations -->

## 8. Testing Support

### 8.1 Add Testing Utilities

```kotlin
class TestChainActor(
    id: String = "test-chain",
    scope: CoroutineScope = TestCoroutineScope()
) : ChainActor(id, scope) {
    val testMemory = TestMemory()
    val testPorts = mutableMapOf<String, TestPort<*>>()

    fun simulateMessage(message: ChainMessage) {
        runBlocking(scope.coroutineContext) {
            processMessage(ActorMessage(payload = message))
        }
    }
}
```


[Back to LangChain Recommendations](LangChain-Recommendations)
