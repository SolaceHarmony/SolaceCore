[← Architecture Overview](../../wiki/Architecture-Overview.md) · §4 of 15

---

## 4. Actor Module (`io.github.solaceharmony.core.actor`)
The `actor` module implements the actor model for concurrent and distributed computation within SolaceCore. It provides abstractions for actors, their states, messages, and basic lifecycle management. The design emphasizes single responsibility, robust message handling, and resource management, leveraging Kotlin coroutines for asynchronous operations.

As per `ACTOR_README.md`, this module is designed with JDK 21+ and Kotlin 2.0.21+ in mind, utilizing features like virtual threads (implied for coroutine dispatchers), string templates, built-in UUIDs, and improved coroutines.

### 4.0. Actor System Design Principles, Status, and Goals
Insights from the wiki [Actor System](../../wiki/Actor-System.md) and [Supervisor & Hot-Swap](../../wiki/Supervisor-and-Hot-Swap.md) pages provide valuable context for the SolaceCore Actor System.

**A. Design Principles**

The actor system is built upon the following core principles:

*   **Isolation:** Actors operate independently, each managing its own state and behavior, inaccessible directly by others.
*   **Message-Driven Communication:** Interaction between actors occurs exclusively through asynchronous message passing via typed ports.
*   **Type Safety:** Communication pathways (ports and messages) are designed to be type-safe, ensuring compatibility and reducing runtime errors.
*   **Concurrency:** Actors are designed to process messages concurrently, leveraging Kotlin coroutines for efficient asynchronous operations.
*   **Lifecycle Management:** Actors adhere to a well-defined lifecycle (`Initialized`, `Running`, `Paused`, `Stopped`, `Error`, `Disposed`), ensuring predictable behavior and resource management.
*   **Error Handling:** The system aims for robust error handling, including timeouts and recovery mechanisms (though detailed recovery strategies beyond basic error states are part of future enhancements).

**B. Noted Technology Considerations**

*   The design checklist for the hot-pluggable system mentions an objective to "Build actor-based architecture using Ktor." While Ktor was not identified as a core dependency in the `:lib` module's `build.gradle.kts`, this indicates a design consideration or potential future integration for aspects like inter-actor communication in a distributed setting or for exposing actor functionalities via network protocols.

**C. Implementation Status Summary**

*   **Completed:**
    *   Basic actor structure (`Actor` class) and lifecycle management.
    *   Port system for type-safe message passing.
    *   Basic error handling and timeout mechanisms within actors.
    *   Collection of performance metrics (`ActorMetrics`).
    *   Dynamic actor registration and unregistration (via `SupervisorActor`).
    *   Hot-swapping capabilities for actors (via `SupervisorActor`).
*   **Partially Implemented / In Progress (at the time of the design documents):**
    *   Advanced queuing mechanisms for messages.
    *   Correlation IDs for tracking tasks across multiple actors.
#### 4.0.1. Conceptual Actor Communication Flow (Sequence Diagram)
*Note: The actor communication sequence diagram and its description have been moved to the wiki [Actor Communication Sequence](../../wiki/Actor-Communication-Sequence.md) page.*
### 4.1. Core Actor Definitions
The foundational components of the actor model are defined in `ActorState.kt`, `ActorMessage.kt`, and `Actor.kt`.

#### 4.1.1. `ActorState` Sealed Class
Defines the possible lifecycle states of an actor.

*   **Purpose:** To represent the distinct operational phases an actor can be in.
*   **States:**
    *   `object Initialized`: Actor is initialized but not yet started.
    *   `object Running`: Actor is active and processing messages.
    *   `object Stopped`: Actor has terminated its processing.
    *   `data class Error(val exception: String)`: Actor encountered an error.
    *   `data class Paused(val reason: String)`: Actor is temporarily paused.
*   Provides an `override fun toString()` for human-readable state representation.

#### 4.1.2. `ActorMessage<T : Any>` Data Class
Represents messages exchanged between actors.

*   **Purpose:** To serve as an immutable data carrier for inter-actor communication.
*   **Generic:** `T` - The type of the payload carried by the message.
*   **Key Properties:**
    *   `correlationId: String`: Unique ID for message tracking (defaults to a random `Uuid.random().toString()`).
    *   `payload: T`: The actual content of the message.
    *   `sender: String?`: Optional identifier of the sending actor.
    *   `timestamp: Long`: Message creation time (defaults to `System.currentTimeMillis()`).
    *   `priority: MessagePriority`: Enum (`HIGH`, `NORMAL`, `LOW`) indicating processing priority (defaults to `NORMAL`).
    *   `metadata: Map<String, Any>`: Additional contextual information (defaults to `emptyMap()`).
*   **Factory Methods:** Companion object provides `highPriority()`, `withMetadata()`, and `between()` for convenient message creation.

#### 4.1.3. `ActorMessageHandler<T : Any>` Abstract Class
A base class for handling specific types of `ActorMessage`s, integrating with the port system.
*   **Implements:** `io.github.solaceharmony.core.kernel.channels.ports.Port.MessageHandler<ActorMessage<T>, ActorMessage<T>>`.
*   **Abstract Method:** `abstract suspend fun processMessage(message: ActorMessage<T>): ActorMessage<T>`.
*   The `handle()` method (required by `Port.MessageHandler`) calls `processMessage()`.

#### 4.1.4. `Actor` Abstract Class
The central abstraction for all actors in the system.

*   **Implements:** `io.github.solaceharmony.core.lifecycle.Lifecycle`.
*   **Constructor:**
    *   `id: String`: Unique actor ID (defaults to `Uuid.random().toString()`).
    *   `name: String`: Human-readable name (defaults to "Actor").
    *   `protected val scope: CoroutineScope`: Coroutine scope for the actor's operations.
*   **State Management:**
    *   Manages its high-level state (`Initialized`, `Running`, `Stopped`, `Error`, `Paused`) via an atomic `_state` variable of type `ActorState`.
    *   Uses an internal `DefaultLifecycle` instance (inner class implementing `Lifecycle`) for basic start/stop/active status.
*   **Port-Based Communication:**
    *   Actors can create and manage typed communication ports using the `createPort<T : Any>(name, messageClass, handler, ...)` method.
    *   Each port is an instance of an inner class `TypedPort<T : Any>`, which wraps a `io.github.solaceharmony.core.kernel.channels.ports.Port<T>` (specifically, a `BidirectionalPort` is created).
    *   The `TypedPort` handles incoming messages on its channel, processes them with a user-provided `handler` lambda, and includes timeout handling (`processingTimeout`) and metrics recording.
    *   Message processing for each port runs in a separate coroutine job, managed by the actor.
    *   Provides methods `getPort()`, `removePort()`, `recreatePort()`, `disconnectPort()`.
*   **Lifecycle Methods & Hooks:**
    *   `start()`: Transitions to `Running` state, starts internal lifecycle, calls `onStart()`.
    *   `stop()`: Transitions to `Stopped` state, stops internal lifecycle, cancels all port processing jobs, disposes all ports, calls `onStop()`.
    *   `pause(reason: String)`: Transitions to `Paused` state.
    *   `resume()`: Transitions back to `Running` from `Paused`.
    *   `dispose()`: Calls `stop()`.
    *   `isActive()`: Reflects the internal lifecycle state.
    *   **Abstract Hooks for Subclasses:**
        *   `protected abstract suspend fun onStart()`
        *   `protected abstract suspend fun onStop()`
        *   `protected abstract suspend fun onMessage(message: ActorMessage<Any>)`: A general message handler, distinct from port-specific handlers.
*   **Direct Message Sending (Abstract):**
    *   `abstract suspend fun send(message: ActorMessage<Any>)`: Indicates actors can send messages, but the mechanism (e.g., to other actors via an actor system) is not defined in this base class.
    *   `suspend fun sendToSelf(message: ActorMessage<Any>)`: Delivers a message to its own `onMessage()` handler.
*   **Error Handling:**
    *   `protected open suspend fun handleMessageProcessingError(error: Throwable, message: Any)`: Hook for errors during port message handling.
    *   `protected open suspend fun onError(error: Throwable, message: Any)`: General error hook.
*   **Metrics:** Contains a `protected val metrics = ActorMetrics()` instance for collecting performance data.

#### 4.1.5. Actor System Core Class Relationships (Diagram)
*Note: The actor system class diagram and its description have been moved to the wiki [Actor System Class Diagram](../../wiki/Actor-System-Class-Diagram.md) page.*
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
### 4.3. Actor Metrics (`metrics` Subdirectory)
The `io.github.solaceharmony.core.actor.metrics` package provides a dedicated class for collecting and managing performance and operational metrics for individual actors.

#### 4.3.1. `ActorMetrics` Class
This class offers a comprehensive suite of metrics to monitor an actor's behavior and performance. Instances of this class are typically held by each `Actor` (as seen in the `Actor` base class).

*   **Purpose:** To track key performance indicators (KPIs) such as message throughput, processing times, error rates, and distribution of messages by protocol, priority, or port.
*   **Key Metrics Collected:**
    *   **General Message Statistics:**
        *   `messagesReceived`: Total count of messages received by the actor.
        *   `messagesProcessed`: Total count of messages successfully processed.
        *   `messagesFailed`: Total count of messages that resulted in processing errors.
        *   (Uses `kotlinx.atomicfu.AtomicLong` for thread-safe counting).
    *   **Protocol-Specific Statistics:** (Stored in `ConcurrentHashMap<String, AtomicLong>`)
        *   Counts of received, processed, and failed messages, broken down by a `protocol` string identifier.
    *   **Processing Time Statistics:**
        *   `lastProcessingTime`: `Duration` of the most recent message processing.
        *   A rolling list (`ArrayList<Long>`) of the last `MAX_PROCESSING_TIMES` (default 1000) processing durations (in milliseconds) is kept to calculate:
            *   `averageProcessingTime`
            *   `maxProcessingTime`
            *   `minProcessingTime`
        *   Access to the `processingTimes` list is synchronized using a `Mutex`.
    *   **Priority-Based Statistics:** (Stored in `ConcurrentHashMap<String, AtomicLong>`)
        *   Counts of messages processed, categorized by their priority level (e.g., "HIGH", "NORMAL", "LOW").
    *   **Port-Specific Statistics:** (Stored in `ConcurrentHashMap<String, AtomicLong>`)
        *   Counts of messages processed through specific actor ports, identified by port name.
*   **Metric Recording Methods:**
    *   `fun recordMessageReceived(protocol: String? = null)`
    *   `fun recordMessageProcessed(protocol: String? = null)`
    *   `fun recordError(protocol: String? = null)`
    *   `suspend fun recordProcessingTime(duration: Duration, port: String? = null)`
    *   `fun recordPriorityMessage(priority: String)`
*   **Metric Retrieval:**
    *   `suspend fun getMetrics(): Map<String, Any>`: Asynchronously compiles and returns a map containing all current metric values. This map includes calculated metrics like `successRate` and organizes protocol, priority, and port metrics into nested maps for clarity.
*   **Reset Functionality:**
    *   `suspend fun reset()`: Resets all collected metrics to their initial zeroed/empty state.

The `ActorMetrics` class is essential for observing the runtime behavior of actors, identifying bottlenecks, and ensuring the overall health of the actor system.
### 4.4. Actor Supervision (`supervisor` Subdirectory)
The `io.github.solaceharmony.core.actor.supervisor` package provides a mechanism for managing groups of actors.

#### 4.4.1. `SupervisorActor` Class
This class is a concrete implementation of `Actor` designed to oversee and manage a collection of other "child" or "managed" actors. Its focus is on dynamic registration, lifecycle control, and hot-swapping of these actors.

*   **Purpose:** To act as a central point for managing a group of actors, allowing for their addition, removal, and replacement at runtime, as well as collective lifecycle operations.
*   **Inheritance:** Extends `io.github.solaceharmony.core.actor.Actor`.
*   **Key Responsibilities and Features:**
    *   **Actor Registry:**
        *   Maintains an internal registry (`actorRegistry: Map<String, Actor>`) of actors it manages, keyed by actor ID.
        *   Also keeps track of the `KClass` of each registered actor (`actorTypeRegistry`) for type-safe operations like hot-swapping.
        *   Access to these registries is synchronized using a `Mutex`.
    *   **Dynamic Actor Management:**
        *   `suspend fun registerActor(actor: Actor): Boolean`: Adds a new actor to its management pool. Requires the `SupervisorActor` to be running.
        *   `suspend fun unregisterActor(actorId: String): Boolean`: Removes an actor from its management pool. Requires the `SupervisorActor` to be running.
        *   `suspend fun hotSwapActor(oldActorId: String, newActor: Actor): Boolean`: Replaces an existing managed actor with a new instance. The process, as detailed in the wiki [SupervisorActor](../../wiki/SupervisorActor.md) page, involves:
            1.  Checking if the `oldActorId` exists in the registry.
            2.  Verifying that the `newActor` is of the same `KClass` as the old one, ensuring type safety.
            3.  Noting if the old actor was in a `Running` state.
            4.  Stopping the old actor.
            5.  Replacing the old actor with the new one in the internal `actorRegistry` and `actorTypeRegistry`.
            6.  Starting the `newActor` if the old actor was previously running.
            This operation requires the `SupervisorActor` itself to be in a `Running` state.
*   **Operational Note:** Most dynamic management operations (`registerActor`, `unregisterActor`, `hotSwapActor`) and collective lifecycle controls (`startAllActors`, `stopAllActors`) require the `SupervisorActor` to be in a `Running` state. Attempting these operations when the supervisor is not running will typically result in an `IllegalStateException`. Furthermore, methods like `registerActor`, `unregisterActor`, and `hotSwapActor` return a `Boolean` value: `true` indicates successful completion, while `false` usually signifies a failure due to conditions such as a non-existent actor ID or a type mismatch during hot-swapping, as noted in the wiki [SupervisorActor](../../wiki/SupervisorActor.md) page.
    *   **Actor Discovery:**
        *   `suspend fun getActor(actorId: String): Actor?`: Retrieves a specific managed actor by its ID.
        *   `suspend fun getAllActors(): List<Actor>`: Returns a list of all actors currently managed by the supervisor.
        *   `suspend fun getActorsByType(actorType: KClass<out Actor>): List<Actor>`: Retrieves all managed actors that are instances of a specific `actorType`.
    *   **Collective Lifecycle Control:**
        *   `suspend fun startAllActors()`: Calls `start()` on all managed actors. Requires the `SupervisorActor` to be running.
        *   `suspend fun stopAllActors()`: Calls `stop()` on all managed actors. Requires the `SupervisorActor` to be running.
    *   **Resource Cleanup (`dispose()`):**
        *   When the `SupervisorActor` itself is disposed, it iterates through all its managed actors in `actorRegistry` and calls `dispose()` on each, then clears its internal registries before calling `super.dispose()`.
##### 4.4.1.1. Best Practices
The wiki [SupervisorActor](../../wiki/SupervisorActor.md) page outlines several best practices for using the `SupervisorActor` effectively:

*   **Initialization Order:** Always start the `SupervisorActor` (by calling its `start()` method) before attempting to register any child actors.
*   **Unique Actor IDs:** Ensure that all actors registered with a supervisor have unique IDs to prevent registration conflicts and ensure predictable behavior.
*   **Hot-Swap Compatibility:** When hot-swapping actors, verify that the new actor instance is not only of the same `KClass` but also logically compatible with the role and connections of the actor it is replacing.
*   **Resource Management:** Dispose of the `SupervisorActor` using its `dispose()` method when it is no longer needed. This ensures that all managed actors are also properly disposed of, releasing their resources.
*   **Fault Tolerance:**
    *   The `SupervisorActor` as defined in this file primarily focuses on lifecycle management and dynamic updates rather than implementing traditional actor supervision strategies (e.g., "one-for-one," "all-for-one" restart/stop strategies upon child failure). Fault handling for individual actors would typically be the responsibility of the actors themselves or a different supervisory layer if more complex strategies are needed.

### 4.5. Actor Usage Examples (`examples` Subdirectory)
The `io.github.solaceharmony.core.actor.examples` package provides concrete examples of how to implement custom actors by extending the `Actor` base class. These examples illustrate common patterns such as message filtering and transformation.

#### 4.5.1. `Filter<T : Any>` Actor
This example demonstrates a generic actor that filters incoming messages based on a user-defined predicate.

*   **Functionality:**
    *   Receives messages of a generic type `T` on an `INPUT_PORT`.
    *   Applies a `predicate: (T) -> Boolean` (provided during construction) to each message.
    *   If the predicate returns `true`, the message is sent to an `ACCEPTED_PORT`.
    *   Optionally, if `includeRejectedPort` is `true` during construction, messages for which the predicate returns `false` are sent to a `REJECTED_PORT`.
*   **Key Implementation Details:**
    *   The constructor takes the `predicate`, `includeRejectedPort` flag, and the `messageClass: KClass<T>` for type-safe port creation.
    *   Ports (`INPUT_PORT`, `ACCEPTED_PORT`, and optionally `REJECTED_PORT`) are created in an `initialize()` method, which is called from an overridden `start()` method to ensure ports are ready before the actor fully starts.
    *   The `INPUT_PORT`'s handler, `filterMessage(message: T)`, contains the core filtering logic and routes messages to the appropriate output port (`ACCEPTED_PORT` or `REJECTED_PORT`) using `getPort(PORT_NAME, messageClass)?.send(message)`.
    *   Output ports (`ACCEPTED_PORT`, `REJECTED_PORT`) are created with empty handlers as their role is solely to emit messages.

```kotlin
// Conceptual structure of the Filter actor's core logic
class Filter<T : Any>(
    // ... constructor parameters including predicate and messageClass ...
) : Actor(...) {
    // ... companion object with port names ...

    suspend fun initialize() {
        createPort(INPUT_PORT, messageClass, handler = ::filterMessage, ...)
        createPort(ACCEPTED_PORT, messageClass, handler = { /* output only */ }, ...)
        if (includeRejectedPort) {
            createPort(REJECTED_PORT, messageClass, handler = { /* output only */ }, ...)
        }
    }

    private suspend fun filterMessage(message: T) {
        if (predicate(message)) {
            getPort(ACCEPTED_PORT, messageClass)?.send(message)
        } else if (includeRejectedPort) {
            getPort(REJECTED_PORT, messageClass)?.send(message)
        }
    }

    override suspend fun start() {
        if (getPort(INPUT_PORT, messageClass) == null) initialize()
        super.start()
    }
    // ... other Actor overrides if necessary ...
}
```

#### 4.5.2. `TextProcessor` Actor
This example showcases an actor that performs a series of transformations on incoming text messages.

*   **Functionality:**
    *   Receives `String` messages on an `INPUT_PORT`.
    *   Applies a list of `transformations: List<(String) -> String>` (provided during construction) sequentially to the input string.
    *   Sends the final processed string to an `OUTPUT_PORT`.
*   **Key Implementation Details:**
    *   The constructor takes a list of transformation functions. The companion object provides several predefined transformations like `TO_UPPERCASE`, `TRIM`, etc.
    *   Ports (`INPUT_PORT` for `String`, `OUTPUT_PORT` for `String`) are created in an `initialize()` method, called from an overridden `start()` method.
    *   The `INPUT_PORT`'s handler, `processText(text: String)`, iterates through the `transformations`, applies them, and then sends the result to the `OUTPUT_PORT`.
    *   The `OUTPUT_PORT` has an empty handler.

```kotlin
// Conceptual structure of the TextProcessor actor's core logic
class TextProcessor(
    // ... constructor parameters including transformations list ...
) : Actor(...) {
    // ... companion object with port names and example transformations ...

    suspend fun initialize() {
        createPort(INPUT_PORT, String::class, handler = ::processText, ...)
        createPort(OUTPUT_PORT, String::class, handler = { /* output only */ }, ...)
    }

    private suspend fun processText(text: String) {
        var processedText = text
        for (transformation in transformations) {
            processedText = transformation(processedText)
        }
        getPort(OUTPUT_PORT, String::class)?.send(processedText)
    }

    override suspend fun start() {
        if (getPort(INPUT_PORT, String::class) == null) initialize()
        super.start()
    }
    // ... other Actor overrides if necessary ...
}
```
These examples illustrate the practical use of the `Actor` base class, its port creation mechanism (`createPort`), message handling via port handlers, and state management through the actor's lifecycle methods. They serve as excellent starting points for developing more complex custom actors.
### 4.6. Future Enhancements and Roadmap
The existing design documents for the actor system also outline several areas for future development and enhancement.

**A. Future Enhancements**

The following capabilities were identified as potential future additions or areas for improvement:

*   **Advanced Error Handling and Recovery (including Supervisor-specific enhancements):** Implementing more sophisticated strategies beyond basic error state reporting, such as:
	    *   Automatic actor restart policies and more general automatic actor recovery after failures (especially for supervised actors, as noted in [SupervisorActor](../../wiki/SupervisorActor.md)).
    *   State rollback mechanisms upon failure.
	    *   Formalized actor supervision strategies (e.g., one-for-one, all-for-one restart/stop strategies, as distinct from the current `SupervisorActor`'s focus on dynamic management), potentially including hierarchical supervision with child supervisors (also from [SupervisorActor](../../wiki/SupervisorActor.md)).
*   **Message Management:**
    *   More advanced message queuing mechanisms (e.g., priority queues, dead-letter queues).
    *   Enhanced message prioritization schemes.
*   **Distributed Actors:** Support for actors running across multiple processes or machines, potentially leveraging technologies like Ktor (as hinted in `Hot-Pluggable_Actor_System.md`).
*   **Monitoring and Management:**
    *   More comprehensive monitoring dashboards.
    *   Advanced management tools for observing and controlling the actor system at runtime.
*   **Security:**
    *   Message encryption options for inter-actor communication.
    *   Access control mechanisms for actor interactions or management operations.
*   **Integration:**
    *   Streamlined integration with external systems like message brokers (e.g., Kafka, RabbitMQ) or databases.
*   **State Management & Persistence (including Supervisor-specific hot-swap enhancements):**
    *   Improved hot-swapping capabilities, specifically:
        *   Support for actor state transfer during hot-swapping.
        *   Dynamic port reconnection after hot-swapping.
        *   General state migration between old and new actor instances.
    *   More advanced actor persistence mechanisms beyond the current `ActorStateStorage`, such as event sourcing patterns.
*   **Testing:**
    *   More comprehensive testing utilities specifically designed for actors and actor systems.

**B. Roadmap Considerations**

The checklist for the "Hot-Pluggable Actor System" also highlighted several key areas of focus for its development, which serve as good general roadmap considerations:

*   Define clear and stable APIs for actor interaction and management.
*   Implement robust error handling and recovery mechanisms (reiterated).
*   Ensure type safety throughout the message passing system.
*   Develop comprehensive documentation for developers and users.
*   Provide illustrative examples and common use cases.
*   Pay close attention to performance characteristics and scalability.
*   Integrate seamlessly with existing logging and monitoring infrastructure.
*   Address security concerns proactively.
*   Plan for versioning and maintain backward compatibility where feasible.
*   A specific item noted was to "Build actor-based architecture using Ktor," suggesting a potential technological direction for network-enabled or distributed actor features.

These points indicate a forward-looking vision for the actor module, aiming to build a robust, scalable, and feature-rich environment for concurrent application development within SolaceCore.
#### 4.7. Advanced Actor Communication & Lifecycle: Queuing, Hibernation, and Correlation
The archived `SolaceCoreFramework.md` document details a sophisticated design for managing actor communication, long-running tasks, and state, which complements the core actor model:

*   **Task Queuing & Correlation Management:**
    *   **Centralized Queue Management:** Envisions a centralized task queue, potentially inspired by systems like BizTalk or message brokers (e.g., RabbitMQ), to manage the full lifecycle of messages. This would be managed by Kotlin Coroutines.
    *   **Correlation ID for State Management:** Each task passing through the queue would be tagged with a Correlation ID. This ID uniquely identifies tasks and is crucial for long-lived processes that might hibernate and resume, ensuring continuity without reprocessing stages.
    *   **Actor-Level Queues:** Each actor would also possess its own local queue, regulated by the central queue manager, to prevent individual actors from being overwhelmed and to optimize local performance. This creates a two-tiered queuing system.

*   **Hibernation and Resuming Actors:**
    *   **Task Hibernation:** For tasks involving waits for external events or responses (e.g., API callbacks, user input), actors are designed to "hibernate." This involves serializing minimal state information (Correlation ID, input data, current step) to persistent storage (e.g., SQLite).
    *   **Hibernation Triggers:** Conditions like waiting for external callbacks or time-based delays.
    *   **Resuming Hibernated Tasks:** The Queue Manager would notify an actor when its resumption condition is met. The actor deserializes its state using the Correlation ID and continues from the saved point. This is compared to Saga patterns in distributed systems.
    *   **Serialization and State Storage:** `kotlinx.serialization` is proposed for lightweight serialization (e.g., to JSON), with SQLite suggested for local persistent storage of hibernated state.

*   **Queue Management Strategies:**
    *   **Priority Queues:** The Queue Manager would handle task prioritization (e.g., health-check actors having higher priority than batch-processing actors).
    *   **Rate Limiting:** To prevent overwhelming specific actors, the queue manager would include rate-limiting capabilities.
    *   **Fallback and Retrying Mechanism:** Failed tasks would be returned to the queue with an incremented retry count. After a set number of retries, tasks could be routed to a fallback actor or an alert/manual intervention queue.

*   **System Communication and Orchestration (Role of Supervisor):**
    *   The Supervisor Actor is key to this queue orchestration, interacting with both central and local queues to assign tasks based on priority and load.
    *   Ktor channels are envisioned for internal communication, with supervisors coordinating worker actors and managing queue-related data.

*   **Future Enhancements for this System:**
    *   **Distributed Queue Management:** Leveraging systems like Kafka, RabbitMQ, or Kotlin's Multik for scaling beyond a single instance.
    *   **Actor Lifecycle Analytics:** Detailed metrics on hibernation, retries, processing times, potentially integrated with Prometheus.
    *   **Callback Channels:** Specialized channels for actors awaiting external events to avoid congesting general-purpose queues.

This design aims to balance efficiency and resilience, enabling actors to handle diverse task types effectively.

---

← [§3 Storage Module (`io.github.solaceharmony.core.storage`)](./03-storage-module-io-github-solaceharmony-core-storage.md)  ·  [Architecture Overview](../../wiki/Architecture-Overview.md)  ·  [§5 Workflow Module (`io.github.solaceharmony.core.workflow`)](./05-workflow-module-io-github-solaceharmony-core-workflow.md) →
