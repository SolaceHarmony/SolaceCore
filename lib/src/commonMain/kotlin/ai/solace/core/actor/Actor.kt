package ai.solace.core.actor

import ai.solace.core.actor.interfaces.ActorInterface
import ai.solace.core.actor.metrics.ActorMetrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlin.uuid.*
import kotlin.time.Duration
import kotlin.time.measureTime
import kotlin.uuid.ExperimentalUuidApi

enum class ActorState {
    INITIALIZED,
    RUNNING,
    STOPPED,
    ERROR
}
/**
 * Abstract class that provides a foundation for an actor-based concurrency model using Kotlin actors.
 */
abstract class Actor(
    @OptIn(ExperimentalUuidApi::class)
    val id: String = Uuid.random().toString(),
    protected val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    @Volatile
    private var _state: ActorState = ActorState.INITIALIZED

    private val state: ActorState
        get() = _state

    protected val metrics = ActorMetrics()

    /**
     * Actor's channel for processing messages asynchronously.
     * Uses Kotlin's actor function to create a coroutine-based message processing loop.
     */
    @OptIn(ObsoleteCoroutinesApi::class)
    private val actorChannel: SendChannel<ActorMessage> = scope.actor(Dispatchers.Default, capacity = Channel.BUFFERED) {
        _state = ActorState.RUNNING
        for (message in channel) {
            try {
                metrics.recordMessageReceived()

                // Measure the processing time using kotlin.time.measureTime
                val processingTime: Duration = measureTime {
                    processMessage(message)
                }

                metrics.recordMessageProcessed()
                // The Duration object already contains the time in milliseconds
                metrics.recordProcessingTime(processingTime)
            } catch (e: Exception) {
                _state = ActorState.ERROR
                handleError(e, message)
                metrics.recordError()
            }
        }
        _state = ActorState.STOPPED
    }

    protected abstract val actorInterface: ActorInterface

    init {
        defineInterface()
    }

    /**
     * Defines the actor's interface by setting up input and output ports as well as tools.
     * This method is intended to be overridden by subclasses to provide specific implementations
     * of the actor's interface.
     */
    protected abstract fun defineInterface()

    /**
     * Starts the actor if it hasn't already been started.
     */
    fun start() {
        if (_state == ActorState.INITIALIZED || _state == ActorState.STOPPED) {
            _state = ActorState.RUNNING
        }
    }

    /**
     * Sends a message to the actor's internal channel.
     *
     * @param message The message to be sent to the actor.
     * @throws kotlin.IllegalStateException if the actor is not running.
     */
    suspend fun send(message: ActorMessage) {
        if (state == ActorState.RUNNING) {
            actorChannel.send(message)
        } else {
            throw IllegalStateException("Cannot send message; actor is not running.")
        }
    }

    /**
     * Processes an incoming message in an actor.
     *
     * Subclasses must override this method to define specific
     * message handling logic.
     *
     * @param message The message to be processed.
     */
    protected abstract suspend fun processMessage(message: ActorMessage)

    /**
     * Handles errors that occur during the processing of actor messages.
     *
     * Subclasses can override this method to provide custom error handling behavior.
     *
     * @param error The exception that was thrown during message processing.
     * @param message The actor message that was being processed when the error occurred.
     */
    protected open fun handleError(error: Exception, message: ActorMessage) {
        println("Error processing message ${message.correlationId}: ${error.message}")
    }

    /**
     * Stops the actor by cancelling its current job and setting it to null.
     * Ensures that no further messages are processed.
     */
    fun stop() {
        actorChannel.close()
        _state = ActorState.STOPPED
    }

    /**
     * Retrieves the current ActorInterface for the actor.
     * This interface provides access to the actor's input ports, output ports, and tools.
     *
     * @return The ActorInterface instance associated with the actor.
     */
    fun getInterface(): ActorInterface = actorInterface

    /**
     * Retrieves the current state of the actor.
     *
     * @return The current state of the actor.
     */
    fun getState(): ActorState = state

    /**
     * Checks if the actor is in a healthy state.
     *
     * @return true if the actor is running and not in an error state, false otherwise.
     */
    fun isHealthy(): Boolean {
        return state == ActorState.RUNNING
    }

    /**
     * Represents a message to be sent or received by an actor.
     *
     * @property correlationId A unique identifier for the message, used for tracing and pairing requests and responses.
     * @property type The type of the message, usually indicating the action to be performed or the nature of the message.
     * @property payload The actual content or data of the message, which can be of any type.
     * @property sender An optional identifier of the sender of the message, useful for identifying the source of the message.
     */
    data class ActorMessage(
        @OptIn(ExperimentalUuidApi::class)
        val correlationId: String = Uuid.random().toString(),
        val type: String,
        val payload: Any,
        val sender: String? = null
    )
}