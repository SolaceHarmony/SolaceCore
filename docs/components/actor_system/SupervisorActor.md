# SupervisorActor

## Overview
The SupervisorActor is a specialized actor responsible for managing the lifecycle of other actors in the system. It provides functionality for dynamic actor registration, unregistration, and hot-swapping, allowing for runtime modification of the actor system without requiring system restarts.

## Features
- **Dynamic Actor Registration**: Register and unregister actors at runtime
- **Hot-Swapping**: Replace actors with new instances of the same type while preserving their connections
- **Type-Safe Actor Management**: Ensure that actors are only replaced with compatible types
- **Thread-Safe Operations**: All operations are synchronized using a mutex to prevent race conditions
- **Lifecycle Management**: Start and stop all managed actors as a group

## Usage

### Creating a SupervisorActor
```kotlin
val supervisor = SupervisorActor()
supervisor.start()
```

### Registering an Actor
```kotlin
val actor = MyActor()
val registered = supervisor.registerActor(actor)
if (registered) {
    println("Actor registered successfully")
} else {
    println("Failed to register actor (duplicate ID)")
}
```

### Unregistering an Actor
```kotlin
val unregistered = supervisor.unregisterActor(actor.id)
if (unregistered) {
    println("Actor unregistered successfully")
} else {
    println("Failed to unregister actor (not found)")
}
```

### Hot-Swapping an Actor
```kotlin
val oldActor = MyActor()
supervisor.registerActor(oldActor)

// Create a new actor with updated behavior
val newActor = MyActor()

// Hot-swap the old actor with the new one
val swapped = supervisor.hotSwapActor(oldActor.id, newActor)
if (swapped) {
    println("Actor hot-swapped successfully")
} else {
    println("Failed to hot-swap actor (not found or incompatible type)")
}
```

### Managing Actor Lifecycles
```kotlin
// Start all actors
supervisor.startAllActors()

// Stop all actors
supervisor.stopAllActors()

// Dispose the supervisor and all managed actors
supervisor.dispose()
```

### Getting Actors
```kotlin
// Get an actor by ID
val actor = supervisor.getActor(actorId)

// Get all actors
val allActors = supervisor.getAllActors()

// Get actors by type
val specificActors = supervisor.getActorsByType(MyActor::class)
```

## Implementation Details

### Actor Registry
The SupervisorActor maintains two registries:
1. `actorRegistry`: Maps actor IDs to actor instances
2. `actorTypeRegistry`: Maps actor IDs to their class types (used for type checking during hot-swapping)

### Thread Safety
All operations on the actor registry are synchronized using a mutex to prevent race conditions when multiple coroutines attempt to register, unregister, or hot-swap actors simultaneously.

### Hot-Swapping Process
When hot-swapping an actor, the SupervisorActor:
1. Checks if the old actor exists in the registry
2. Verifies that the new actor is of the same type as the old one
3. Checks if the old actor is running
4. Stops the old actor
5. Replaces the old actor with the new one in the registry
6. Starts the new actor if the old one was running

### Error Handling
The SupervisorActor throws `IllegalStateException` if operations are attempted while it is not in the `Running` state. It also returns `false` for operations that fail due to missing actors or type incompatibility.

## Best Practices
- Always start the SupervisorActor before registering actors
- Ensure that actors have unique IDs to prevent registration conflicts
- When hot-swapping actors, ensure that the new actor is compatible with the old one
- Dispose the SupervisorActor when it is no longer needed to clean up resources

## Future Enhancements
- Support for actor state transfer during hot-swapping
- Dynamic port reconnection after hot-swapping
- Hierarchical supervision with child supervisors
- Automatic actor recovery after failures