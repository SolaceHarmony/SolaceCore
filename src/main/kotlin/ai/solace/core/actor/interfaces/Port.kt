package ai.solace.core.actor.interfaces

import kotlinx.coroutines.channels.Channel
import kotlin.reflect.KClass

sealed class Port<T : Any>(val name: String, val type: KClass<T>) {
    class Input<T : Any>(name: String, type: KClass<T>) : Port<T>(name, type) {
        internal var channel: Channel<T>? = null
        
        suspend fun receive(): T? = channel?.receive()
    }

    class Output<T : Any>(name: String, type: KClass<T>) : Port<T>(name, type) {
        private val connections = mutableListOf<Channel<T>>()
        
        suspend fun send(value: T) {
            connections.forEach { channel ->
                channel.send(value)
            }
        }

        internal fun connect(channel: Channel<T>) {
            connections.add(channel)
        }
    }

    class Tool<T : Any>(name: String, type: KClass<T>) : Port<T>(name, type) {
        private var implementation: (suspend (T) -> Any)? = null
        
        fun implement(handler: suspend (T) -> Any) {
            implementation = handler
        }

        suspend fun invoke(param: T): Any {
            return implementation?.invoke(param) 
                ?: throw IllegalStateException("Tool $name has no implementation")
        }
    }
}