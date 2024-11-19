package ai.solace.core.channels

import ai.solace.core.common.Disposable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Registry for managing and tracking ports in the system
 */
class PortRegistry : Disposable {
    private val mutex = Mutex()
    private val inputs = mutableMapOf<String, InputPort<*>>()
    private val outputs = mutableMapOf<String, OutputPort<*>>()

    suspend fun registerInput(port: InputPort<*>) = mutex.withLock {
        inputs[port.id] = port
    }

    suspend fun registerOutput(port: OutputPort<*>) = mutex.withLock {
        outputs[port.id] = port
    }

    suspend fun findInput(id: String): InputPort<*>? = mutex.withLock {
        inputs[id]
    }

    suspend fun findOutput(id: String): OutputPort<*>? = mutex.withLock {
        outputs[id]
    }

    suspend fun listPorts(): Map<String, List<Port<*>>> = mutex.withLock {
        mapOf(
            "inputs" to inputs.values.toList(),
            "outputs" to outputs.values.toList()
        )
    }

    override suspend fun dispose() = mutex.withLock {
        inputs.values.forEach { it.dispose() }
        outputs.values.forEach { it.dispose() }
        inputs.clear()
        outputs.clear()
    }
}