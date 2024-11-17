package ai.solace.core.actor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class SupervisorActor(scope: CoroutineScope) : Actor(id = "supervisor", scope = scope) {
    private val actors = ConcurrentHashMap<String, Actor>()
    private val mutex = Mutex()

override fun defineInterface() {
    // Define the interface for the SupervisorActor if needed
}
    override suspend fun processMessage(message: ActorMessage) {
        when (message.type) {
            "RegisterActor" -> registerActor(message.payload as Actor)
            "UnregisterActor" -> unregisterActor(message.payload as String)
            "GetActorInfo" -> handleGetActorInfo(message)
            else -> handleError(IllegalArgumentException("Unknown message type: ${message.type}"), message)
        }
    }

    private suspend fun registerActor(actor: Actor) = mutex.withLock {
        actors[actor.id] = actor
        actor.start()
    }

    private suspend fun unregisterActor(actorId: String) = mutex.withLock {
        actors[actorId]?.let { actor ->
            actor.stop()
            actors.remove(actorId)
        }
    }

    private suspend fun handleGetActorInfo(message: ActorMessage) {
        val actorId = message.payload as String
        val actor = actors[actorId]
        // Here we would send back actor information to the requester
        // This will be implemented when we add the response channel mechanism
    }

    fun getRegisteredActors(): List<String> = actors.keys.toList()

    override fun handleError(error: Exception, message: ActorMessage) {
        println("Supervisor error processing message ${message.correlationId}: ${error.message}")
        // In a real implementation, we would log this and potentially take recovery actions
    }
}