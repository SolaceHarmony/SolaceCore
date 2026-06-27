<!-- topic: Runtime -->
<!-- title: Kernel Port Implementations and Exceptions -->

#### 1.1.3. Concrete Implementations and Utilities
The `io.github.solaceharmony.core.kernel.channels.ports` package also provides concrete implementations and utilities.

##### 1.1.3.1. `BidirectionalPort<T : Any>` Class
A concrete implementation of the `Port<T>` interface that supports both sending and receiving messages.
*   **Implements:** `Port<T>`.
*   **Constructor:** `name: String`, `id: String = Port.generateId()`, `type: KClass<out T>`, `bufferSize: Int = Channel.BUFFERED`.
*   **Key Features:**
    *   Manages an internal `kotlinx.coroutines.channels.Channel<T>`.
    *   Allows registration of `Port.MessageHandler<T, T>` instances via `addHandler()`.
    *   Allows registration of `Port.ConversionRule<T, T>` instances via `addConversionRule()`.
    *   The `send(message: T)` method applies registered handlers and conversion rules sequentially before sending to the internal channel.
    *   Provides a `suspend fun receive(): T` method to receive messages from the internal channel.
    *   Implements `dispose()` by closing the internal channel.
*   **Companion Object (`BidirectionalPort.Companion`):**
    *   `inline fun <reified T : Any> create(name: String, id: String = Port.generateId()): BidirectionalPort<T>`: Factory method.

##### 1.1.3.2. `StringProtocolAdapter<T : Any>` Class
An `open class` providing a base for protocol adapters that convert to/from `String`.
*   **Implements:** `Port.ProtocolAdapter<T, String>`.
*   **`encode(source: T): String`:** Converts the source object to its string representation (`source.toString()`).
*   **`decode(target: String): T`:** Throws `UnsupportedOperationException`; meant to be implemented by concrete subclasses.
*   **`canHandle(sourceType: KClass<*>, targetType: KClass<*>) : Boolean`:** Returns `true` if `targetType` is `String::class`.
*   **Companion Object (`StringProtocolAdapter.Companion`):**
    *   `inline fun <reified T : Any> create(crossinline decoder: (String) -> T): Port.ProtocolAdapter<T, String>`: Factory method that creates an anonymous subclass overriding `decode` and refining `canHandle`.

#### 1.1.4. Exception Handling
The system defines a hierarchy of `internal` custom exceptions for port-related errors, all extending a base `PortException`.

*   **`internal open class PortException(message: String, cause: Throwable? = null) : Exception(message, cause)`**
    The base class for all port-specific exceptions.

*   **`internal class PortException.Validation(message: String, cause: Throwable? = null) : PortException(message, cause)`**
    Thrown during validation failures, such as in type conversion or message handling within a port.

*   **`internal class PortConnectionException(val sourceId: String, val targetId: String, message: String, details: Map<String, Any> = emptyMap(), cause: Throwable? = null) : PortException(...)`**
    Thrown when establishing a connection between two ports fails (e.g., due to incompatible types, failing protocol adapter, or invalid conversion rule chain). Includes `sourceId` and `targetId`.

*   **`internal class SendMessageException(message: String, cause: Throwable? = null) : PortException(message, cause)`**
    Thrown if an error occurs specifically during the message sending process through a port.

#### 1.1.5. Design Principles
The architecture of the Channel System adheres to the following core principles (as outlined in `CHANNELS_README.md` and reflected in the code):

1.  **Distributed First:**
    *   State sharing between components is minimized.
    *   Operations are designed to be decentralized.
    *   Message passing mechanisms aim for low overhead.

2.  **Resource Safety:**
    *   The `Disposable` interface ensures that resources are properly released when a port is no longer needed (e.g., `BidirectionalPort.dispose()` closes its channel).
    *   Lifecycles of ports and connections are actively managed.
    *   Connection handling is designed to be robust and prevent resource leaks.

3.  **Type Safety:**
    *   Leverages Kotlin's type system (`KClass`, generics) for compile-time checks.
    *   Includes runtime type verification where necessary (e.g., in `PortConnection.canConnect()`, `Port.ConversionRule.canHandle()`).
    *   Provides clear and informative error messages for type mismatches or other type-related issues via custom exceptions.



[Back to Kernel & Ports](Kernel-and-Ports)
