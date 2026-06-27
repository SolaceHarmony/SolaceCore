<!-- topic: Orientation -->
<!-- title: Framework Concurrency and Communication -->

## 8. Concurrency and Communication

### 8.1 Coroutine Usage

Kotlin coroutines provide the foundation for asynchronous processing:

- **Actor Scopes**: Each actor operates within its own coroutine scope
- **Asynchronous Processing**: Non-blocking message handling using suspend functions
- **Structured Concurrency**: Managing the lifecycle of concurrent operations

### 8.2 Channel-Based Communication

Coroutine channels enable type-safe message passing between actors:

- **Channel Types**: Different channel types (buffered, conflated, etc.) for various use cases
- **Flow Control**: Handling backpressure and preventing overflow
- **Message Ordering**: Preserving message order when required

### 8.3 Concurrency Control

The framework includes mechanisms for controlling concurrency:

- **Mutex**: For thread-safe access to shared resources
- **Rate Limiting**: Preventing actors from being overwhelmed by messages
- **Timeout Handling**: Managing long-running operations with timeouts



[Back to Solace Core Framework Architecture](Solace-Core-Framework-Architecture)
