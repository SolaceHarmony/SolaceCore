package ai.solace.core.builder

import ai.solace.core.actor.Actor
import ai.solace.core.actor.interfaces.Port
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KClass
import kotlinx.coroutines.channels.Channel

class ActorBuilder<A : Actor>(
    private val actorClass: KClass<A>,
    private val scope: CoroutineScope
) {
    private val connections = mutableListOf<Connection<*>>()
    private var configuration: Map<String, Any> = emptyMap()

    data class Connection<T : Any>(
        val fromActor: Actor,
        val fromPort: String,
        val toActor: Actor,
        val toPort: String,
        val type: KClass<T>
    )

    fun configure(config: Map<String, Any>): ActorBuilder<A> {
        configuration = config
        return this
    }

    fun <T : Any> connectTo(
        fromActor: Actor,
        fromPort: String,
        toActor: Actor,
        toPort: String,
        type: KClass<T>
    ): ActorBuilder<A> {
        connections.add(Connection(fromActor, fromPort, toActor, toPort, type))
        return this
    }

    @Suppress("UNCHECKED_CAST")
    fun build(): A {
        // Create the actor instance
        val constructor = actorClass.constructors.first()
        val actor = if (constructor.parameters.isEmpty()) {
            constructor.call()
        } else {
            constructor.call(scope)
        }

        // Apply connections
        connections.forEach { conn ->
            val outputPort = conn.fromActor.getInterface().getOutput(conn.fromPort)
                ?: throw IllegalStateException("Output port ${conn.fromPort} not found in actor ${conn.fromActor.id}")
            
            val inputPort = conn.toActor.getInterface().getInput(conn.toPort)
                ?: throw IllegalStateException("Input port ${conn.toPort} not found in actor ${conn.toActor.id}")

            if (outputPort.type != conn.type || inputPort.type != conn.type) {
                throw IllegalStateException("Type mismatch in connection: ${outputPort.type} -> ${inputPort.type}")
            }

            (outputPort as Port.Output<Any>).connect(inputPort.channel as Channel<Any>)
        }

        return actor
    }
}