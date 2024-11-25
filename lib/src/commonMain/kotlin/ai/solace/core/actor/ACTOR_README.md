
## Platform Requirements

### JDK Requirements
- JDK 21+
  - Virtual Thread support
  - Enhanced pattern matching
  - String templates

### Kotlin Requirements
- Kotlin 2.0.21+
  - Built-in UUID support
  - Improved coroutines
  - Enhanced type inference

## Best Practices

### 1. Actor Design
- Keep actors focused on single responsibilities
- Use meaningful actor IDs
- Handle all potential message types
- Implement proper error handling

### 2. Message Handling
```kotlin
override suspend fun processMessage(message: ActorMessage) {
    try {
        when (message.type) {
            "Known" -> handleKnown(message)
            else -> handleUnknown(message)
        }
    } catch (e: Exception) {
        handleError(e, message)
    }
}
```

### 3. Resource Management
- Properly clean up resources in stop()
- Use structured concurrency
- Monitor actor health
- Track metrics

## Testing Guidelines
1. Unit test individual actors
2. Test message processing flows
3. Verify state transitions
4. Check error handling
5. Monitor metrics collection

## Future Enhancements
1. Generic type parameters for message payloads
2. Advanced routing capabilities
3. Enhanced metrics and monitoring
4. Multi-instance support
5. Native kernel implementation