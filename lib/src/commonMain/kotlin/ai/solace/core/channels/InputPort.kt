package ai.solace.core.channels

import kotlinx.coroutines.channels.Channel
import kotlin.reflect.KClass

/**
 * Input port implementation for receiving messages
 */
class InputPort<T : Any>(
    override val name: String,
    override val type: KClass<T>,
    override val id: String = Port.generateId(),
    private val channel: Channel<T> = Channel(Channel.BUFFERED)
) : Port<T> {
    suspend fun receive(): T = channel.receive()

    override suspend fun dispose() {
        channel.cancel()
    }

    override fun toString() = "InputPort(id=$id, name=$name, type=${type.simpleName})"

    override fun equals(other: Any?) = other is InputPort<*> && other.id == id
    override fun hashCode() = id.hashCode()
}