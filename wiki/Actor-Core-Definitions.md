<!-- topic: Runtime -->
<!-- title: Actor Core Definitions -->

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
*Note: The actor system class diagram and its description have been moved to the wiki [Actor System Class Diagram](Actor-System-Class-Diagram) page.*


[Back to Actor System](Actor-System)
