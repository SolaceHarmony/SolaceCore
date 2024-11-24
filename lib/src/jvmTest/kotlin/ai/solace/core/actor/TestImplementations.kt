package ai.solace.core.actor

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import ai.solace.core.kernel.channels.ports.Port

/**
 * Base test actor for common test functionality
 */
open class TestActor(id: String, name: String) : Actor(id, name) {
    /**
     * Creates a test port with the specified configuration
     */
    fun <T : Any> testCreatePort(
        name: String,
        type: Any,
        protocolAdapter: Port.ProtocolAdapter<*, *>? = null,
        handlers: List<Port.MessageHandler<*, *>> = emptyList()
    ): Port<T> = createPort(name, type, protocolAdapter, handlers as List<Port.MessageHandler<T, T>>)

    /**
     * Creates a test port specifically for actor messages
     */
    fun <T : Any> testCreateActorPort(
        name: String,
        messageType: Any
    ): Port<ActorMessage<T>> = createActorPort(name, messageType)

    /**
     * Sends a test actor message through the specified port
     */
    suspend fun <T : Any> testSendActorMessage(
        port: Port<ActorMessage<T>>,
        payload: T,
        priority: MessagePriority = MessagePriority.NORMAL,
        metadata: Map<String, Any> = emptyMap()
    ) = sendActorMessage(port, payload, priority, metadata)

    override suspend fun onActorMessage(message: ActorMessage<Any>) {
        // Default implementation for tests
    }
}

/**
 * Actor implementation that records received messages for testing
 */
class RecordingActor(id: String, name: String) : TestActor(id, name) {
    private val messageChannel = Channel<ActorMessage<*>>(Channel.UNLIMITED)
    private val recordedMessages = mutableListOf<ActorMessage<*>>()
    private val mutex = Mutex()
    private var processingJob: Job? = null

    /**
     * Receives and records a message
     */
    suspend fun receiveMessage(message: ActorMessage<*>) {
        coroutineScope {
            if (isActive()) {
                mutex.withLock {
                    messageChannel.send(message)
                    recordedMessages.add(message)
                }
            }
        }
    }

    override suspend fun onActorMessage(message: ActorMessage<Any>) {
        receiveMessage(message)
    }

    /**
     * Access recorded messages
     */
    suspend fun getRecordedMessages(): List<ActorMessage<*>> = mutex.withLock {
        recordedMessages.toList()
    }

    /**
     * Clear recorded messages
     */
    suspend fun clearRecordings() = mutex.withLock {
        recordedMessages.clear()
    }

    /**
     * Get messages by port
     */
    suspend fun getMessagesByPort(portName: String): List<ActorMessage<*>> = mutex.withLock {
        val port = getPort<ActorMessage<*>>(portName)
        recordedMessages.filter { message ->
            port?.id == message.metadata["portId"]
        }
    }

    override suspend fun start() {
        super.start()
        coroutineScope {
            processingJob = launch {
                try {
                    while (isActive()) {
                        messageChannel.receiveCatching().getOrNull()?.let { message ->
                            mutex.withLock {
                                recordedMessages.add(message)
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (isActive()) throw e
                }
            }
        }
    }

    override suspend fun stop() {
        processingJob?.cancel()
        super.stop()
    }
}