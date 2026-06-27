<!-- topic: Runtime -->
<!-- title: Actor Usage Examples -->

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


[Back to Actor System](Actor-System)
