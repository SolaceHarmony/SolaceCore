package ai.solace.core.actor.interfaces

import ai.solace.core.common.Lifecycle
import ai.solace.core.channels.InputPort
import ai.solace.core.channels.OutputPort
import ai.solace.core.actor.connections.ConnectionManager
import ai.solace.core.channels.Port
import ai.solace.core.common.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass

class ActorInterface(private val scope: CoroutineScope) : Lifecycle {
    private val connectionManager = ConnectionManager(scope)
    private val ports = mutableMapOf<String, Port<*>>()
    // private val tools = mutableMapOf<String, Tool<*, *>>()
    private val stateMutex = Mutex()
    private var active = false

    override suspend fun start() {
        stateMutex.withLock {
            active = true
        }
    }

    override suspend fun stop() {
        stateMutex.withLock {
            active = false
        }
    }

    override fun isActive(): Boolean = active

    override suspend fun dispose() {
        stop()
        connectionManager.dispose()
        ports.values.filterIsInstance<Disposable>().forEach { it.dispose() }
        ports.clear()
        // tools.clear()
    }

    fun <T : Any> input(name: String, type: KClass<T>): InputPort<T> {
        return InputPort(name, type, channel = Channel(Channel.BUFFERED)).also {
            ports[name] = it
        }
    }

    fun <T : Any> output(name: String, type: KClass<T>): OutputPort<T> {
        return OutputPort(name, type, channel = Channel(Channel.BUFFERED)).also {
            ports[name] = it
        }
    }

    suspend fun <T : Any> connect(
        output: OutputPort<T>,
        input: InputPort<T>,
        bufferSize: Int = Channel.BUFFERED
    ): String {
        require(isActive()) { "ActorInterface must be started before connecting ports" }
        return connectionManager.connect(output, input, bufferSize)
    }

    companion object {
        const val DEFAULT_BUFFER_SIZE = Channel.BUFFERED
    }
}