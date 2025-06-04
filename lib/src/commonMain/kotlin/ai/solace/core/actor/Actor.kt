@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
package ai.solace.core.actor

import ai.solace.core.kernel.channels.ports.*
import ai.solace.core.actor.metrics.ActorMetrics
import ai.solace.core.lifecycle.Lifecycle
import ai.solace.core.lifecycle.Disposable
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.Duration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.reflect.KClass

/**
 * Abstract class representing an actor in the system.
 *
 * @param id Unique identifier of the actor, defaults to a random UUID.
 * @param name Name of the actor, defaults to "Actor".
 * @param scope Coroutine scope used by the actor, defaults to a new scope with default dispatcher and supervisor job.
 */
abstract class Actor(
    val id: String = Uuid.random().toString(),
    var name: String = "Actor",
    protected val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : Lifecycle {
    /**
     * Manages the lifecycle of the Actor class.
     *
     * This variable uses the DefaultLifecycle implementation to control and monitor
     * the state of an actor instance, ensuring it follows the correct lifecycle
     * stages such as starting, stopping, and checking if it's active.
     */
    private val lifecycle = DefaultLifecycle()
    /**
     * Represents the atomic state of the actor, managing its lifecycle states.
     *
     * Initial state is [ActorState.Initialized].
     * Can transition through various states such as [ActorState.Running],
     * [ActorState.Stopped], [ActorState.Error], and [ActorState.Paused].
     */
    private val _state = atomic<ActorState>(ActorState.Initialized)
    /**
     * Retrieves the current state of the actor.
     *
     * The state can be one of the following:
     * - `ActorState.Initialized`: The actor has been initialized but not yet started.
     * - `ActorState.Running`: The actor is currently running and processing messages.
     * - `ActorState.Stopped`: The actor has been stopped.
     * - `ActorState.Error`: The actor encountered an error, with details in the exception.
     * - `ActorState.Paused`: The actor is paused, with a reason provided.
     */
    val state: ActorState
        get() = _state.value

    /**
     * Stores comprehensive performance metrics of the actor.
     * This includes counters for received, processed, and failed messages,
     * as well as protocol-specific and port-specific metrics.
     */
    protected val metrics = ActorMetrics()
    /**
     * A mutex used to protect the access to the ports managed by an actor.
     * It ensures that operations on ports are thread-safe by preventing
     * concurrent modifications and access.
     */
    private val portsMutex = Mutex()
    /**
     * A mutable map that stores named ports associated with an actor.
     * The keys are port names represented as strings, and the values are `TypedPort`
     * instances which are typed ports capable of handling messages of a specific type.
     *
     * Used within the `Actor` class to manage and route messages to the correct ports.
     */
    private val ports = mutableMapOf<String, TypedPort<*>>()
    /**
     * Mutex used for synchronizing access to the `jobs` field in the `Actor` class.
     *
     * Ensures that only one coroutine at a time can access or modify the `jobs` list,
     * preventing concurrent modification issues and maintaining thread safety.
     */
    private val jobsMutex = Mutex()
    /**
     * A mutable list of Job instances representing the collection of jobs being managed by the Actor.
     *
     * This list is used to keep track of coroutine jobs that the Actor has spawned and is managing,
     * typically for various asynchronous tasks. The list allows the Actor to maintain a reference
     * to these jobs for purposes such as cancellation, status monitoring, or to ensure proper
     * cleanup when the Actor is stopped.
     */
    private val jobs = mutableListOf<Job>()

    /**
     * Default implementation of the `Lifecycle` interface.
     * Manages the lifecycle state with a mutex to ensure thread safety.
     */
    private class DefaultLifecycle : Lifecycle {
        /**
         * A Mutex used to ensure thread-safety and synchronization when modifying
         * the state of the lifecycle. This prevents concurrent modifications and
         * ensures that state transitions such as start and stop are performed
         * atomically and safely within the coroutine context.
         */
        private val stateMutex = Mutex()
        /**
         * Indicates whether the lifecycle is currently running.
         *
         * This variable is used to track the active state of the lifecycle.
         * It is set to `true` when the lifecycle is started and changed back to `false` when stopped.
         */
        private var isRunning = false

        /**
         * Starts the lifecycle component.
         *
         * This method acquires a lock on `stateMutex` to ensure thread safety,
         * and changes the internal state `isRunning` to `true`, indicating that the lifecycle
         * component has started running.
         *
         * The method is `suspend` as it performs a potentially blocking operation via the lock.
         *
         * @throws IllegalStateException if the lock could not be acquired
         */
        override suspend fun start() = stateMutex.withLock {
            isRunning = true
        }

        /**
         * Stops the lifecycle by changing its state to not running.
         *
         * This method is thread-safe and ensures that the state change
         * is safely applied, preventing concurrent state mutations.
         */
        override suspend fun stop() = stateMutex.withLock {
            isRunning = false
        }

        /**
         * Determines if the current lifecycle is active.
         *
         * @return `true` if the lifecycle is running, `false` otherwise.
         */
        override fun isActive(): Boolean = isRunning

        /**
         * Disposes of the lifecycle, ensuring that it stops any ongoing activities.
         *
         * This method is safe to call from any coroutine context, as it switches to a
         * `NonCancellable` context to ensure it completes. It primarily aims to change
         * the lifecycle state by invoking the `stop` method, which transitions the state
         * to inactive and releases any held resources.
         */
        override suspend fun dispose() = withContext(NonCancellable) {
            stop()
        }
    }

    /**
     * TypedPort is an inner class responsible for handling typed messages through a given port.
     *
     * @param T The type of messages that this port processes.
     * @property port The port associated with this TypedPort instance.
     * @property messageClass The class of the message type T.
     * @property handler The suspend function to handle incoming messages.
     * @property bufferSize The size of the buffer for message processing.
     * @property processingTimeout The duration after which message processing will timeout.
     */
    protected inner class TypedPort<T : Any>(
        val port: Port<T>,
        private val messageClass: KClass<T>,
        private val handler: suspend (T) -> Unit,
        private val bufferSize: Int = Channel.BUFFERED,  // Remove @Suppress
        private val processingTimeout: Duration = DEFAULT_PROCESSING_TIMEOUT
    ) {
        init {
            require(port.type == messageClass) {
                "Port type ${port.type} doesn't match message type $messageClass"
            }
            require(bufferSize > 0) {
                "Buffer size must be positive, got $bufferSize"
            }
        }
        /**
         * Sends a message through the port.
         *
         * @param message The message to send through the port.
         * @throws IllegalStateException if the actor is not in the Running state.
         * @throws IllegalArgumentException if the message type does not match the port type.
         */
        @Suppress("unused")
        suspend fun send(message: T) {
            require(messageClass.isInstance(message)) {
                "Message type ${message::class} doesn't match port type $messageClass"
            }
            if (state != ActorState.Running) {
                throw IllegalStateException("Cannot send message while actor is in state: $state")
            }
            port.send(message)
        }

        /**
         * Initiates the processing of messages received on the specified port.
         *
         * This function launches a coroutine that processes messages as long as the actor's state is set to `Running`.
         * If the state changes or an error occurs during processing, appropriate handling is performed.
         *
         * @return The Job representing the coroutine that processes the messages.
         */
        fun startProcessing(): Job = scope.launch {
            try {
                for (message in port.asChannel()) {
                    if (state == ActorState.Running) {
                        processMessageWithTimeout(message)
                    }
                }
            } catch (e: Exception) {
                handlePortError(port.name, e)
            }
        }

        /**
         * Processes the given message with a specified timeout.
         *
         * @param message The message to be processed.
         */
        private suspend fun processMessageWithTimeout(message: T) {
            try {
                withTimeout(processingTimeout) {
                    processMessage(message)
                }
            } catch (_: TimeoutCancellationException) {
                // Using underscore since we're not using the exception parameter
                handleProcessingTimeout(message)
            }
        }

        /**
         * Processes a single message, recording relevant metrics and handling errors.
         *
         * @param message The message to be processed.
         */
        private suspend fun processMessage(message: T) {
            try {
                metrics.recordMessageReceived()
                val processingTime = measureTime {
                    handler(message)
                }
                metrics.recordMessageProcessed()
                metrics.recordProcessingTime(processingTime)
            } catch (e: Exception) {
                handleMessageProcessingError(e, message)
            }
        }

        /**
         * Handles the scenario when the processing of a message exceeds the allowed timeout duration.
         *
         * @param message The message that failed to process within the given timeframe.
         */
        private suspend fun handleProcessingTimeout(message: T) {
            val error = TimeoutCancellationException("Message processing timed out for ${messageClass.simpleName}")
            handleMessageProcessingError(error, message)
        }
    }

    /**
     * Creates a new port that listens for messages of type [T] and processes them using the provided handler.
     *
     * @param name The unique name of the port to be created.
     * @param messageClass The class type of the messages that this port will handle.
     * @param handler A suspend function to handle incoming messages of type [T].
     * @param bufferSize The buffer size for the port's channel. Defaults to [Channel.BUFFERED].
     * @param processingTimeout The maximum duration for processing a single message before timing out. Defaults to [DEFAULT_PROCESSING_TIMEOUT].
     * @return The created [Port] corresponding to the specified parameters.
     * @throws PortException.Validation if a port with the specified name already exists.
     */
    suspend fun <T : Any> createPort(
        name: String,
        messageClass: KClass<T>,
        handler: suspend (T) -> Unit,
        bufferSize: Int = Channel.BUFFERED,
        processingTimeout: Duration = DEFAULT_PROCESSING_TIMEOUT
    ): Port<T> = portsMutex.withLock {
        if (ports.containsKey(name)) {
            throw PortException.Validation("Port with name $name already exists")
        }

        val port = BidirectionalPort(
            name = name,
            type = messageClass,
            bufferSize = bufferSize  // Pass through the buffer size
        )
        val typedPort = TypedPort(
            port = port,
            messageClass = messageClass,
            handler = handler,
            bufferSize = bufferSize,
            processingTimeout = processingTimeout
        )
        ports[name] = typedPort

        jobsMutex.withLock {
            val processingJob = typedPort.startProcessing()
            jobs.add(processingJob)
        }

        port
    }

    /**
     * Retrieves a port by its name and expected message type.
     *
     * @param name The name of the port to retrieve.
     * @param messageClass The expected class type of the messages that the port handles.
     * @return The port corresponding to the provided name and message class, or null if no such port exists.
     */
    @Suppress("UNCHECKED_CAST", "unused")
    suspend fun <T : Any> getPort(
        name: String,
        messageClass: KClass<T>
    ): Port<T>? = portsMutex.withLock {
        val typedPort = ports[name]
        if (typedPort?.port?.type == messageClass) {
            typedPort.port as Port<T>
        } else {
            null
        }
    }


    /**
     * Removes a port from the actor.
     *
     * This method stops the processing job associated with the port, disposes the port,
     * and removes it from the actor's port registry.
     *
     * @param name The name of the port to remove.
     * @return true if the port was successfully removed, false if the port doesn't exist.
     * @throws IllegalStateException if the actor is not in a state that allows port removal.
     */
    suspend fun removePort(name: String): Boolean {
        if (state != ActorState.Running && state != ActorState.Initialized && state !is ActorState.Paused) {
            throw IllegalStateException("Cannot remove port while actor is in state: $state")
        }

        return portsMutex.withLock {
            val typedPort = ports[name] ?: return@withLock false

            // Cancel all jobs and recreate them for the remaining ports
            jobsMutex.withLock {
                // Cancel all jobs
                jobs.forEach { it.cancel() }
                jobs.clear()

                // Restart processing for all ports except the one being removed
                ports.forEach { (portName, port) ->
                    if (portName != name) {
                        val processingJob = port.startProcessing()
                        jobs.add(processingJob)
                    }
                }
            }

            // Dispose the port
            typedPort.port.dispose()

            // Remove the port from the registry
            ports.remove(name)
            true
        }
    }

    /**
     * Recreates a port with a new handler, effectively disconnecting it from all its connections.
     *
     * This method removes the existing port and creates a new one with the same name and type,
     * but with a new handler. This is useful for changing the behavior of a port without
     * changing its connections.
     *
     * @param name The name of the port to recreate.
     * @param messageClass The class type of the messages that the port handles.
     * @param handler The new handler for the port.
     * @param bufferSize The buffer size for the new port's channel. Defaults to [Channel.BUFFERED].
     * @param processingTimeout The maximum duration for processing a single message before timing out. Defaults to [DEFAULT_PROCESSING_TIMEOUT].
     * @return The newly created port, or null if the port doesn't exist or the message class doesn't match.
     * @throws IllegalStateException if the actor is not in a state that allows port recreation.
     * @throws PortException.Validation if the port removal fails.
     */
    suspend fun <T : Any> recreatePort(
        name: String,
        messageClass: KClass<T>,
        handler: suspend (T) -> Unit,
        bufferSize: Int = Channel.BUFFERED,
        processingTimeout: Duration = DEFAULT_PROCESSING_TIMEOUT
    ): Port<T>? {
        if (state != ActorState.Running && state != ActorState.Initialized && state !is ActorState.Paused) {
            throw IllegalStateException("Cannot recreate port while actor is in state: $state")
        }

        // Check if the port exists and has the correct type
        val existingPort = getPort(name, messageClass) ?: return null

        // Remove the existing port
        if (!removePort(name)) {
            throw PortException.Validation("Failed to remove port $name")
        }

        // Create a new port with the same name and type but a new handler
        return createPort(name, messageClass, handler, bufferSize, processingTimeout)
    }


    /**
     * Disconnects a port from all its connections.
     *
     * This method removes the existing port and creates a new one with the same name and type,
     * but with a new handler that does nothing. This effectively disconnects the port from all
     * its connections while maintaining its registration with the actor.
     *
     * @param name The name of the port to disconnect.
     * @param messageClass The class type of the messages that the port handles.
     * @return The newly created port, or null if the port doesn't exist or the message class doesn't match.
     * @throws IllegalStateException if the actor is not in a state that allows port disconnection.
     */
    suspend fun <T : Any> disconnectPort(
        name: String,
        messageClass: KClass<T>
    ): Port<T>? {
        if (state != ActorState.Running && state != ActorState.Initialized && !(state is ActorState.Paused)) {
            throw IllegalStateException("Cannot disconnect port while actor is in state: $state")
        }

        // Check if the port exists and has the correct type
        val existingPort = getPort(name, messageClass) ?: return null

        // Remove the existing port
        if (!removePort(name)) {
            throw PortException.Validation("Failed to remove port $name")
        }

        // Create a new port with the same name and type but a handler that does nothing
        return createPort(
            name = name,
            messageClass = messageClass,
            handler = { /* Do nothing */ },
            bufferSize = Channel.BUFFERED,
            processingTimeout = DEFAULT_PROCESSING_TIMEOUT
        )
    }

    /**
     * Handles errors that occur during message processing within an actor.
     *
     * @param error The exception that was thrown during message processing.
     * @param message The message that was being processed when the error occurred.
     */
    protected open suspend fun handleMessageProcessingError(error: Throwable, message: Any) {
        when (error) {
            is CancellationException -> throw error
            is PortException -> handlePortError("unknown", error)
            else -> {
                _state.value = ActorState.Error(error.message ?: "Unknown error")
                metrics.recordError()
                onError(error, message)
            }
        }
    }

    /**
     * Handles errors related to a specific port, updates the actor state to Error, records the error,
     * and throws a PortException.Validation.
     *
     * @param portName The name of the port where the error occurred.
     * @param error The exception that was thrown during the port operation.
     */
    private fun handlePortError(portName: String, error: Exception) {
        _state.value = ActorState.Error("Port $portName failure: ${error.message}")
        metrics.recordError()
        throw PortException.Validation("Port $portName channel failed: ${error.message}")
    }

    /**
     * Handles errors occurring during message processing.
     *
     * @param error The exception that was thrown and caused the error.
     * @param message The message that was being processed when the error occurred.
     */
    protected open suspend fun onError(error: Throwable, message: Any) {
        metrics.recordError()
    }

    /**
     * Starts the actor, transitioning its state to `Running` if it was in an
     * `Initialized` or `Stopped` state.
     * This method also initiates the actor's lifecycle.
     *
     * Override in subclasses to provide specific start-up behavior.
     */
    override suspend fun start() {
        lifecycle.start()
        if (state == ActorState.Initialized || state == ActorState.Stopped) {
            _state.value = ActorState.Running
        }
    }

    /**
     * Stops the actor, transitioning its state to `ActorState.Stopped`.
     *
     * This method performs the following actions:
     *
     * 1. Stops the lifecycle of the actor.
     * 2. Updates the internal state to `ActorState.Stopped`.
     * 3. Cancels all active jobs associated with the actor.
     * 4. Disposes all ports associated with the actor.
     */
    override suspend fun stop() {
        lifecycle.stop()
        _state.value = ActorState.Stopped
        jobsMutex.withLock {
            jobs.forEach { it.cancel() }
        }
        portsMutex.withLock {
            ports.values.forEach { it.port.dispose() }
        }
    }

    /**
     * Checks whether the actor is currently active.
     *
     * An actor is considered active if its lifecycle is active and its state is `Running`.
     *
     * @return `true` if the actor is active, `false` otherwise.
     */
    override fun isActive(): Boolean =
        lifecycle.isActive() && state == ActorState.Running

    /**
     * Disposes the resources held by the actor.
     *
     * This method performs the following steps:
     * 1. Stops the actor.
     * 2. Cancels and waits for all jobs associated with the actor.
     * 3. Disposes all ports associated with the actor.
     * 4. Cancels the coroutine scope and the actor's lifecycle.
     *
     * All the above operations are performed within a `NonCancellable` context to ensure they complete without interruption.
     */
    override suspend fun dispose() {
        withContext(NonCancellable) {
            try {
                stop()
                jobsMutex.withLock {
                    jobs.forEach { it.cancelAndJoin() }
                }
                portsMutex.withLock {
                    Disposable.dispose(*ports.values.map { it.port }.toTypedArray())
                }
            } finally {
                scope.cancel()
                lifecycle.dispose()
            }
        }
    }

    /**
     * Retrieves all current metrics.
     * @return Map of metric names to values
     */
    @Suppress("unused")
    suspend fun getMetrics(): Map<String, Any> = metrics.getMetrics()

    /**
     * Suspends the actor's execution, transitioning it to a paused state.
     *
     * @param reason The reason why the actor is being paused.
     */
    @Suppress("unused", "RedundantSuspendModifier")
    suspend fun pause(reason: String) {
        if (state == ActorState.Running) {
            _state.value = ActorState.Paused(reason)
        }
    }

    /**
     * Resumes the actor if it is currently in a Paused state.
     *
     * This method transitions the actor's state from `Paused` to `Running`.
     */
    @Suppress("unused", "RedundantSuspendModifier")
    suspend fun resume() {
        if (state is ActorState.Paused) {
            _state.value = ActorState.Running
        }
    }

    /**
     * Companion object for the Actor class.
     *
     * This object holds constants and utility methods related to the Actor class.
     */
    companion object {
        /**
         * The default duration within which message processing should be completed.
         *
         * This value is typically used in scenarios where the system needs to enforce a time constraint
         * on message handling operations to ensure timely processing and avoid potential timeouts.
         *
         * The default value is set to 30 seconds.
         */
        val DEFAULT_PROCESSING_TIMEOUT: Duration = Duration.parse("PT30S") // 30 seconds
    }
}

/**
 * Exception thrown when a timeout occurs that leads to the cancellation of a coroutine or operation.
 *
 * @constructor Creates a new TimeoutCancellationException with the specified message.
 * @param message The detail message for this exception.
 */
class TimeoutCancellationException(message: String) : CancellationException(message)
