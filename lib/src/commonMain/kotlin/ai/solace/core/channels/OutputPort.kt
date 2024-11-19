package ai.solace.core.channels

import kotlinx.coroutines.channels.Channel
import kotlin.reflect.KClass

/**
 * Output port implementation for sending messages
 */
class OutputPort<T : Any>(
    override val name: String,
    override val type: KClass<T>,
    override val id: String = Port.generateId(),
    private val channel: Channel<T> = Channel(Channel.BUFFERED)
) : Port<T> {
    suspend fun send(value: T) = channel.send(value)

    override suspend fun dispose() {
        channel.cancel()
    }

    override fun toString() = "OutputPort(id=$id, name=$name, type=${type.simpleName})"

    override fun equals(other: Any?) = other is OutputPort<*> && other.id == id
    override fun hashCode() = id.hashCode()
}