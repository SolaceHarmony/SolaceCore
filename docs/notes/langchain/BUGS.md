# Known Bugs and Issues

## Actor System Issues

### 1. Potential Race Condition in Actor State Management
- In `Actor.kt`, there's a potential race condition between checking the state and sending a message in the `send()` method
- The check `if (state == ActorState.RUNNING)` and the subsequent `actorChannel.send(message)` are not atomic
- A state change could occur between these operations
- **Fix**: Consider using atomic operations or implementing proper state synchronization

### 2. Incomplete Error State Recovery
- When an actor enters the ERROR state, there's no built-in mechanism to recover
- The error state is set in the message processing loop but there's no way to reset it
- **Fix**: Implement a recovery mechanism or error reset functionality

### 3. Memory Leak Potential in Message Processing
- No explicit buffer size limits on the actor channel (`Channel.BUFFERED`)
- Could lead to out-of-memory issues under high load
- **Fix**: Implement backpressure mechanisms or configurable buffer sizes

### 4. Inadequate Resource Cleanup
- The `stop()` method only closes the channel but doesn't ensure all resources are properly cleaned up
- No proper handling of in-flight messages when stopping
- **Fix**: Implement a proper shutdown sequence with resource cleanup

## Port System Issues

### 5. Port ID Generation Weakness
- The Port ID generation in `Port.kt` uses Random.nextBytes(8) which might not guarantee uniqueness
- Could potentially lead to ID collisions in large systems
- **Fix**: Consider using UUIDs or a more robust ID generation system

### 6. Missing Port Validation
- No validation of port types when making connections
- Could lead to runtime type mismatches
- **Fix**: Add compile-time or runtime type checking for port connections

## Testing Issues

### 7. Incomplete Test Coverage
- The `ActorTest.kt` lacks tests for:
  - Concurrent message processing
  - Memory leak scenarios
  - Port connection edge cases
  - Interface modification after actor initialization
- **Fix**: Add comprehensive test cases for these scenarios

### 8. Test Setup Issues
- Tests use `UnconfinedTestDispatcher` which might mask real-world timing issues
- Not all error conditions are tested
- **Fix**: Add tests with realistic dispatchers and more error scenarios

## Documentation Issues

### 9. API Documentation Gaps
- Missing documentation for error handling strategies
- Unclear documentation about thread safety guarantees
- No clear documentation about the lifecycle management
- **Fix**: Add comprehensive API documentation including thread safety and lifecycle details

### 10. Architecture-Implementation Mismatch
- Some features mentioned in the docs/ folder (like clustering) are not implemented in the current codebase
- The actual implementation is simpler than what's described in the documentation
- **Fix**: Either implement the missing features or update the documentation to match the current implementation

## Design Issues

### 11. Limited Configuration Options
- Actor configuration options are minimal
- No way to configure:
  - Message buffer sizes
  - Processing timeouts
  - Error handling strategies
- **Fix**: Implement a comprehensive configuration system

### 12. Missing Metrics Features
- `ActorMetrics` implementation is basic
- No export functionality for metrics
- No integration with external monitoring systems
- **Fix**: Enhance metrics system with more features and external system integration

## Build and Dependency Issues

### 13. Experimental API Usage
- Heavy use of experimental Kotlin features (e.g., `@ExperimentalUuidApi`, `@ObsoleteCoroutinesApi`)
- May cause issues with future Kotlin versions
- **Fix**: Either remove dependency on experimental features or document upgrade strategy

## Notes:
- These issues are based on the current codebase as of November 2024
- Some issues might be intended design decisions rather than bugs
- Priority should be given to issues affecting stability and correctness

## Recommendations for Immediate Action:
1. Address the race conditions in state management
2. Implement proper resource cleanup
3. Add comprehensive error recovery mechanisms
4. Improve test coverage
5. Update documentation to match implementation

Please update this document as issues are resolved or new issues are discovered.