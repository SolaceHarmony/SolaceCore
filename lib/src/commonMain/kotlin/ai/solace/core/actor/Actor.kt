@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
package ai.solace.core.actor

import ai.solace.core.kernel.channels.ports.*
import ai.solace.core.actor.metrics.ActorMetrics
import ai.solace.core.lifecycle.Lifecycle
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import java.util.concurrent.ConcurrentHashMap
import java.util.Collections
import kotlin.reflect.KClass

/**
 * A base class for defining actors, which are components designed to handle
 * messages asynchronously using Kotlin coroutines.
 */
abstract class Actor(
    val id: String = Uuid.random().toString(),
    var name: String = "Actor",
    protected val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : Lifecycle {

    private val _state = atomic<ActorState>(ActorState.Initialized)
    val state: ActorState
        get() = _state.value

    protected val metrics = ActorMetrics()

    private val ports = ConcurrentHashMap<String, Port<*>>()
    private val jobs = Collections.synchronizedList(mutableListOf<Job>())

    /**
     * Enhanced port implementation with type-safe message handling
     */
    private inner class EnhancedPort<T : Any>(
        override val name: String,
        override val type: KClass<T>,
        private val protocolAdapter: Port.ProtocolAdapter<T, T>?,
        private val messageHandlers: List<Port.MessageHandler<T, T>>
    ) : Port<T> {
        override val id: String = Port.generateId()
        private val channel: Channel<T> = Channel(Channel.BUFFERED)

        override fun asChannel(): Channel<T> = channel

        override suspend fun dispose() {
            withContext(NonCancellable) {
                channel.close()
            }
        }

        suspend fun processMessage(message: T): T {
            var processed = message
            for (handler in messageHandlers) {
                processed = handler.handle(processed)
            }
            return processed
        }

        suspend fun adaptMessage(message: T): T {
            return protocolAdapter?.let { adapter ->
                adapter.decode(adapter.encode(message))
            } ?: message
        }
    }

    /**
     * Creates a new communication port with type safety.
     */
    protected fun <T : Any> createPort(
        name: String,
        type: Any,
        protocolAdapter: Port.ProtocolAdapter<T, T>? = null,
        handlers: List<Port.MessageHandler<T, T>> = emptyList()
    ): Port<T> {
        if (ports.containsKey(name)) {
            throw PortException("Port with name $name already exists")
        }

        protocolAdapter?.let { adapter ->
            validateProtocolAdapter(adapter, type)
        }

        return EnhancedPort(name, type, protocolAdapter, handlers).also { port ->
            ports[name] = port
            val job = scope.launch {
                try {
                    for (message in port.asChannel()) {
                        if (state == ActorState.Running) {
                            try {
                                metrics.recordMessageReceived()
                                val processingTime = measureTime {
                                    processMessage(port, message)
                                }
                                metrics.recordMessageProcessed()
                                metrics.recordProcessingTime(processingTime)
                            } catch (e: Exception) {
                                handleMessageProcessingError(e, message)
                            }
                        }
                    }
                } catch (e: Exception) {
                    handlePortError(port.name, e)
                }
            }
            jobs.add(job)
        }
    }

    /**
     * Processes a message using the provided port with type safety.
     */
    private suspend fun <T : Any> processMessage(port: Port<T>, message: T) {
        when (port) {
            is EnhancedPort<T> -> {
                val processedMessage = port.processMessage(message)
                val adaptedMessage = port.adaptMessage(processedMessage)

                if (adaptedMessage is ActorMessage<*>) {
                    @Suppress("UNCHECKED_CAST")
                    onActorMessage(adaptedMessage as ActorMessage<Any>)
                } else {
                    onGenericMessage(adaptedMessage)
                }
            }
            else -> onGenericMessage(message)
        }
    }

    /**
     * Validates if the given protocol adapter can handle the specified type.
     */
    private fun validateProtocolAdapter(adapter: Port.ProtocolAdapter<*, *>, type: Any) {
        require(adapter.canHandle(type, type)) {
            "Protocol adapter cannot handle type: $type"
        }
    }

    /**
     * Creates a communication port specifically for actor messages.
     */
    protected fun <T : Any> createActorPort(
        name: String,
        messageType: Any
    ): Port<ActorMessage<T>> = createPort(
        name = name,
        type = messageType,
        handlers = listOf(createDefaultActorMessageHandler())
    )

    /**
     * Creates a default message handler for actor messages.
     */
    private fun <T : Any> createDefaultActorMessageHandler():
            Port.MessageHandler<ActorMessage<T>, ActorMessage<T>> =
        object : ActorMessageHandler<T>() {
            override suspend fun processMessage(
                message: ActorMessage<T>
            ): ActorMessage<T> = message
        }

    /**
     * Retrieves a port by its name with type safety.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getPort(name: String): Port<T>? = ports[name] as? Port<T>

    /**
     * Sends a message through the specified port.
     */
    protected suspend fun <T : Any> send(port: Port<T>, message: T) {
        try {
            if (state == ActorState.Running) {
                when (port) {
                    is EnhancedPort<T> -> {
                        val adaptedMessage = port.adaptMessage(message)
                        port.asChannel().send(adaptedMessage)
                    }
                    else -> port.asChannel().send(message)
                }
            } else {
                throw IllegalStateException("Cannot send message while actor is in state: $state")
            }
        } catch (e: Exception) {
            handleSendError(port, e)
        }
    }

    /**
     * Sends an actor message through the specified port.
     */
    protected suspend fun <T : Any> sendActorMessage(
        port: Port<ActorMessage<T>>,
        payload: T,
        priority: MessagePriority = MessagePriority.NORMAL,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val message = ActorMessage(
            payload = payload,
            sender = id,
            priority = priority,
            metadata = metadata
        )
        send(port, message)
    }

    protected open suspend fun handleMessageProcessingError(error: Throwable, message: Any) {
        when (error) {
            is CancellationException -> throw error
            is PortException -> handlePortError("unknown", error)
            else -> {
                _state.value = ActorState.Error(error.message ?: "Unknown error")
                metrics.recordError()
                handleError(error as Exception, message)
            }
        }
    }

    private fun handlePortError(portName: String, error: Exception) {
        _state.value = ActorState.Error("Port $portName failure: ${error.message}")
        metrics.recordError()
        throw PortException("Port $portName channel failed: ${error.message}")
    }

    private fun handleSendError(port: Port<*>, error: Exception) {
        _state.value = ActorState.Error(error.message ?: "Send failure")
        metrics.recordError()
        throw PortConnectionException(
            sourceId = id,
            targetId = port.id,
            message = error.message ?: "Send failure"
        )
    }

    protected abstract suspend fun onActorMessage(message: ActorMessage<Any>)

    protected open suspend fun onGenericMessage(message: Any) {}

    protected open fun handleError(error: Exception, message: Any) {
        metrics.recordError()
    }

    override suspend fun start() {
        if (state == ActorState.Initialized || state == ActorState.Stopped) {
            _state.value = ActorState.Running
        }
    }

    override suspend fun stop() {
        _state.value = ActorState.Stopped
        jobs.forEach { it.cancel() }
        ports.values.forEach { it.dispose() }
    }

    override fun isActive(): Boolean = state == ActorState.Running

    override suspend fun dispose() {
        withContext(NonCancellable) {
            try {
                stop()
                jobs.forEach { it.cancelAndJoin() }
                ports.values.forEach { it.dispose() }
            } finally {
                scope.cancel()
            }
        }
    }

    fun pause(reason: String) {
        if (state == ActorState.Running) {
            _state.value = ActorState.Paused(reason)
        }
    }

    fun resume() {
        if (state is ActorState.Paused) {
            _state.value = ActorState.Running
        }
    }

    suspend fun getMetrics(): Map<String, Any> = metrics.getMetrics()
}