<!-- topic: Reference -->
<!-- title: LangChain Testing Improvements -->

## Testing Improvements

### 1. Actor Testing
**Current Issue:**
```kotlin
// Complex test setup
@Test
fun `test actor processing`() = runTest {
    val actor = TestActor()
    actor.start()
    actor.send(message)
    // Complex verification
}
```

**Proposed Solution:**
```kotlin
// Test DSL
@Test
fun `test actor processing`() = actorTest {
    val actor = testActor {
        processesMessage<DataMessage> { msg ->
            // Processing logic
        }
    }

    actor.receive(DataMessage("test")) {
        expectOutput("result", ProcessedData("test"))
        expectMetric("processing_time") { it < 100.milliseconds }
    }
}
```

### 2. Mock Actor Creation
**Current Issue:**
```kotlin
// Manual mock creation
class MockActor : Actor() {
    override fun processMessage(msg: ActorMessage) {
        // Mock implementation
    }
}
```

**Proposed Solution:**
```kotlin
// Mock DSL
val mockActor = mockActor {
    onMessage("data") { msg ->
        reply(ProcessedData(msg.payload))
    }
    onMessage("control") {
        throw ControlException("test")
    }
}
```



[Back to LangChain Usage Design Improvements](LangChain-Usage-Design-Improvements)
