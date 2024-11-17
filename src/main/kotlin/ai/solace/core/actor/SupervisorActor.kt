package ai.solace.core.actor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class SupervisorActor(scope: CoroutineScope) : Actor(id = "supervisor", scope = scope) {
    private val actors = ConcurrentHashMap<String, Actor>()
    private val mutex = Mutex()
    private val messageHandlers = mutableMapOf<String, (ActorMessage) -> Unit>()
    private val messageQueue = Channel<ActorMessage>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (message in messageQueue) {
                messageHandlers[message.type]?.invoke(message)
            }
        }
    }

    override suspend fun processMessage(message: ActorMessage) {
        when (message.type) {
            "RegisterActor" -> registerActor(message.payload as Actor)
            "UnregisterActor" -> unregisterActor(message.payload as String)
            "GetActorInfo" -> handleGetActorInfo(message)
            else -> handleError(IllegalArgumentException("Unknown message type: ${message.type}"), message)
        }
    }

    override fun defineInterface() {
        // Example of loading a script dynamically (Assuming we have a Kotlin scripting setup)
        val scriptEngine = createKotlinScriptEngine()
        scriptEngine.eval("""
            bindings["supervisorActor"] = this

            // Define dynamic interactions or configurations for the SupervisorActor
            supervisorActor.on("CustomMessage") { message ->
                println("Handling custom message: ${'$'}{message.payload}")
            }
        """)
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

    suspend fun routeMessage(message: ActorMessage) {
        messageQueue.send(message)
    }

    fun on(messageType: String, handler: (ActorMessage) -> Unit) {
        // Map the messageType to a handler for dynamic execution
        messageHandlers[messageType] = handler
    }

    fun getRegisteredActors(): List<String> = actors.keys.toList()

    fun syncStateWithCluster() {
        // Example synchronization process, specifics would depend on your clustering mechanism
        val clusterNodes = getClusterNodes()
        for (node in clusterNodes) {
            node.syncState(getCurrentState())
        }
    }

    fun getCurrentState(): Map<String, Any> {
        // Serialize current state of the actors
        return actors.mapValues { it.value.serializeState() }
    }

    override fun handleError(error: Exception, message: ActorMessage) {
        println("Supervisor error processing message ${message.correlationId}: ${error.message}")
        // In a real implementation, we would log this and potentially take recovery actions
    }
}