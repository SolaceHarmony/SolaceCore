# Fix Proposals for SolaceCore Issues

This document provides detailed proposals for fixing the issues identified in BUGS.md. Each proposal includes code examples, implementation strategies, and potential trade-offs.

## 1. Actor System Fixes

### 1.1 Fix Race Condition in State Management
```kotlin
class Actor {
    private val stateMutex = Mutex()
    private val _state = AtomicReference(ActorState.INITIALIZED)

    suspend fun send(message: ActorMessage) {
        stateMutex.withLock {
            if (_state.get() == ActorState.RUNNING) {
                withTimeout(5000L) { // configurable timeout
                    actorChannel.send(message)
                }
            } else {
                throw IllegalStateException("Cannot send message; actor is not running. Current state: ${_state.get()}")
            }
        }
    }
}
```

### 1.2 Implement Error Recovery
```kotlin
class Actor {
    suspend fun recover() {
        stateMutex.withLock {
            if (_state.get() == ActorState.ERROR) {
                // Perform cleanup
                clearPendingMessages()
                resetMetrics()
                _state.set(ActorState.INITIALIZED)
                // Restart actor
                start()
            }
        }
    }

    private suspend fun clearPendingMessages() {
        // Clear any pending messages in the channel
        while (actorChannel.tryReceive().isSuccess) {
            metrics.recordDroppedMessage()
        }
    }
}
```

### 1.3 Memory Management and Backpressure
```kotlin
class Actor(
    private val config: ActorConfig = ActorConfig()
) {
    data class ActorConfig(
        val channelCapacity: Int = 100,
        val processingTimeout: Duration = 30.seconds,
        val backpressureStrategy: BackpressureStrategy = BackpressureStrategy.BUFFER
    )

    private val actorChannel = Channel<ActorMessage>(
        capacity = when (config.backpressureStrategy) {
            BackpressureStrategy.BUFFER -> config.channelCapacity
            BackpressureStrategy.DROP -> Channel.CONFLATED
            BackpressureStrategy.BLOCK -> Channel.RENDEZVOUS
        }
    )

    suspend fun send(message: ActorMessage) {
        withTimeout(config.processingTimeout) {
            stateMutex.withLock {
                if (_state.get() == ActorState.RUNNING) {
                    when (config.backpressureStrategy) {
                        BackpressureStrategy.DROP -> actorChannel.trySend(message)
                        else -> actorChannel.send(message)
                    }
                }
            }
        }
    }
}
```

### 1.4 Proper Resource Cleanup
```kotlin
class Actor : AutoCloseable {
    private val resources = mutableListOf<AutoCloseable>()
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun close() {
        runBlocking {
            stateMutex.withLock {
                _state.set(ActorState.STOPPED)
                
                // Cancel all ongoing operations
                cleanupScope.launch {
                    actorChannel.close()
                    
                    // Wait for pending messages to complete with timeout
                    withTimeout(5000L) {
                        while (!actorChannel.isEmpty) {
                            delay(100)
                        }
                    }
                    
                    // Cleanup resources in reverse order
                    resources.asReversed().forEach { resource ->
                        try {
                            resource.close()
                        } catch (e: Exception) {
                            // Log cleanup error but continue
                            metrics.recordCleanupError()
                        }
                    }
                }.join()
            }
        }
    }
}
```

## 2. Port System Improvements

### 2.1 Robust Port ID Generation
```kotlin
interface Port<T : Any> {
    companion object {
        private val idCounter = AtomicLong(0)
        
        fun generateId(): String = buildString {
            append("port-")
            append(System.currentTimeMillis())
            append("-")
            append(idCounter.incrementAndGet())
            append("-")
            append(UUID.randomUUID().toString().take(8))
        }
    }
}
```

### 2.2 Type-Safe Port Connections
```kotlin
class PortConnection<T : Any> {
    inline fun <reified S : T> connect(
        output: OutputPort<S>,
        input: InputPort<T>
    ) {
        // Verify type compatibility at runtime
        require(input.type.isInstance(output.type)) {
            "Type mismatch: Cannot connect ${output.type} to ${input.type}"
        }
        
        // Proceed with connection
    }
}
```

## 3. Testing Improvements

### 3.1 Comprehensive Test Suite
```kotlin
@Test
fun `test concurrent message processing`() = runTest {
    val actor = TestActor()
    val messages = List(100) { index ->
        Actor.ActorMessage(type = "test", payload = "Message $index")
    }
    
    actor.start()
    
    coroutineScope {
        messages.forEach { message ->
            launch {
                actor.send(message)
            }
        }
    }
    
    // Verify all messages were processed
    assertEquals(100, actor.processedCount)
    assertTrue(actor.processedMessages.containsAll(messages))
}

@Test
fun `test memory pressure`() = runTest {
    val actor = TestActor(ActorConfig(channelCapacity = 10))
    val largeMsgCount = 1000
    
    actor.start()
    
    // Should not throw OOM
    repeat(largeMsgCount) {
        actor.send(Actor.ActorMessage(type = "test", payload = ByteArray(1024 * 1024)))
    }
    
    assertTrue(actor.metrics.droppedMessages > 0)
}
```

## 4. Configuration System

### 4.1 Comprehensive Configuration
```kotlin
data class ActorConfiguration(
    val id: String = UUID.randomUUID().toString(),
    val processingConfig: ProcessingConfig = ProcessingConfig(),
    val metricConfig: MetricConfig = MetricConfig(),
    val errorConfig: ErrorConfig = ErrorConfig()
) {
    data class ProcessingConfig(
        val channelCapacity: Int = 100,
        val processingTimeout: Duration = 30.seconds,
        val backpressureStrategy: BackpressureStrategy = BackpressureStrategy.BUFFER,
        val maxConcurrentMessages: Int = 1
    )
    
    data class MetricConfig(
        val enabled: Boolean = true,
        val exporterClass: KClass<out MetricExporter>? = null,
        val samplingRate: Double = 1.0
    )
    
    data class ErrorConfig(
        val maxRetries: Int = 3,
        val retryBackoff: Duration = 1.seconds,
        val errorHandler: suspend (Exception, ActorMessage) -> Unit = { _, _ -> }
    )
}
```

## 5. Metrics Enhancement

### 5.1 Enhanced Metrics System
```kotlin
class ActorMetrics {
    private val messageLatency = histogram("message_processing_latency")
    private val messageCount = counter("message_count")
    private val errorCount = counter("error_count")
    private val memoryUsage = gauge("memory_usage")
    
    fun export(): MetricsSnapshot {
        return MetricsSnapshot(
            latencies = messageLatency.snapshot(),
            counts = messageCount.value,
            errors = errorCount.value,
            memory = memoryUsage.value
        )
    }
    
    // Prometheus integration
    fun toPrometheusFormat(): String {
        return buildString {
            appendLine("# HELP actor_message_latency Message processing latency")
            appendLine("# TYPE actor_message_latency histogram")
            // Add metric data in Prometheus format
        }
    }
}
```

## Implementation Priority and Timeline

1. **Immediate (Week 1-2)**
   - Fix race conditions in state management
   - Implement proper resource cleanup
   - Add basic error recovery

2. **Short-term (Week 3-4)**
   - Enhance configuration system
   - Implement backpressure mechanisms
   - Improve port type safety

3. **Medium-term (Month 2)**
   - Enhance metrics system
   - Add comprehensive test suite
   - Implement monitoring integration

4. **Long-term (Month 3+)**
   - Implement advanced features from documentation
   - Performance optimization
   - Documentation updates

## Migration Strategy

1. Create new versions of affected classes with `V2` suffix
2. Deprecate old implementations
3. Provide migration guides
4. Allow grace period for updates
5. Remove deprecated code in next major version

## Testing Strategy

1. Unit tests for all new implementations
2. Integration tests for actor interactions
3. Performance tests for memory and concurrency
4. Migration tests for backward compatibility

## Notes

- All changes maintain backward compatibility where possible
- New features are introduced behind feature flags
- Performance impact is measured before/after changes
- Documentation is updated alongside code changes

## Future Considerations

1. Consider adding support for distributed actors
2. Plan for Kotlin 2.0 migration
3. Evaluate integration with other actor frameworks
4. Consider implementing supervision hierarchies

Please update this document as implementation progresses and new requirements are discovered.