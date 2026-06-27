<!-- topic: Runtime -->
<!-- title: Actor Builder -->

### 4.2. Actor Construction (`builder` Subdirectory)
The `io.github.solaceharmony.core.actor.builder` package provides a fluent API for constructing networks of actors and defining their interconnections.

#### 4.2.1. `ActorBuilder` Class
*   **Purpose:** To offer a type-safe and readable way to:
    1.  Instantiate and add actors.
    2.  Define the types of messages their ports handle.
    3.  Establish connections between actor ports.
    4.  Ultimately produce a configured `WorkflowManager` instance that manages the constructed actor network.
*   **Key Components & Workflow (Target Asynchronous Architecture):**
    *   **Internal `WorkflowManager`:** The `ActorBuilder` internally creates and configures an instance of `io.github.solaceharmony.core.workflow.WorkflowManager`.
    *   **Actor Registration (`suspend fun addActor()`):**
        *   The preferred method for adding an `Actor` is the `suspend fun addActor(actor, portDefinitions)`.
        *   When an `Actor` instance is added, it's stored internally by the builder.
        *   The `portDefinitions` (a `Map<String, KClass<*>>` mapping port names to their expected message types) are recorded for later connection validation.
        *   The actor is then added to the internal `WorkflowManager` instance by directly calling the `workflowManager.addActor(actor)` suspend function, maintaining asynchronicity.
    *   **Port Connection (`suspend fun connect()`):**
        *   The preferred method `suspend fun connect(sourceActor, sourcePort, targetActor, targetPort)` establishes a link between specified ports.
        *   It performs a type check: if `portDefinitions` were provided, it verifies `KClass` message type compatibility, throwing an `IllegalArgumentException` if incompatible.
        *   The actual connection declaration is delegated to the `workflowManager.connectActors(...)` suspend function.
    *   **Building the Network (`build()`):**
        *   The `build()` method (called after all actors and connections are defined) returns the configured `WorkflowManager` instance, ready for execution.
*   **Usage (Recommended Asynchronous Approach):**
    *   The target architectural pattern involves obtaining an `ActorBuilder` instance via the `suspend fun buildActorNetworkAsync()` top-level function within a coroutine scope.
    *   Then, chain calls to the `suspend` versions of `addActor()` and `connect()`.
    *   Finally, call `build()` to get the `WorkflowManager`.

```kotlin
// Conceptual Usage (Target Asynchronous Architecture):
// Assuming this code is within a coroutine scope (e.g., inside a suspend function or launched coroutine)
val actorBuilder = buildActorNetworkAsync() // Preferred way to get the builder

val actorA = MyActorImpl("actorA")
val actorB = AnotherActorImpl("actorB")

actorBuilder
    .addActor(actorA, mapOf("outPort" to String::class, "errPort" to Exception::class)) // Uses suspend fun
    .addActor(actorB, mapOf("inPort" to String::class)) // Uses suspend fun
    .connect(actorA, "outPort", actorB, "inPort") // Uses suspend fun
    // ... add more actors and connections

val workflowManager = actorBuilder.build()
// workflowManager can now be started or used to manage the actor network.
```

*   **Note on Blocking Calls and Deprecation:**
    *   The `ActorBuilder` also provides deprecated blocking variants: `addActorBlocking(...)`, `connectBlocking(...)`, and a non-suspending `buildActorNetwork()`.
    *   These methods internally use `runBlocking` to bridge calls to the `WorkflowManager`'s suspend functions.
    *   **Architectural Guidance:** While provided for backward compatibility or specific synchronous contexts, the use of these blocking calls (and thus `runBlocking`) within the builder is **not recommended for new asynchronous designs**. The target architecture strongly favors the `suspend` functions (`buildActorNetworkAsync`, `addActor`, `connect`) to ensure end-to-end non-blocking behavior and prevent potential deadlocks or performance issues associated with `runBlocking` in a concurrent environment.

This builder, when used with its asynchronous API, simplifies the setup of complex actor topologies by abstracting the direct interactions with the `WorkflowManager` during the configuration phase while preserving the benefits of Kotlin's coroutine system.


[Back to Actor System](Actor-System)
