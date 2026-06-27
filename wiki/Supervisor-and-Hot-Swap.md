<!-- topic: Runtime -->

## SupervisorActor

The SupervisorActor is a specialized actor responsible for managing the lifecycle of other actors in the system. It provides functionality for dynamic actor registration, unregistration, and hot-swapping, allowing for runtime modification of the actor system without requiring system restarts.

Key features:
- Dynamic actor registration and unregistration at runtime
- Hot-swapping actors with new instances of the same type
- Type-safe actor management
- Thread-safe operations using a mutex
- Lifecycle management for all managed actors

For more details, see [SupervisorActor](SupervisorActor).
