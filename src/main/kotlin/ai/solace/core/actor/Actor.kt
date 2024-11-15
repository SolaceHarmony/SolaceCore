package ai.solace.core.actor

import ai.solace.core.actor.interfaces.ActorInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.UUID

abstract class Actor(
    val id: String = UUID.randomUUID().toString(),
    protected val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private var job: Job? = null
    protected val internalChannel = Channel<ActorMessage>(Channel.BUFFERED)
    protected val actorInterface = ActorInterface()
    
    data class ActorMessage(
        val correlationId: String = UUID.randomUUID().toString(),
        val type: String,
        val payload: Any,
        val sender: String? = null
    )

    init {
        defineInterface()
    }

    protected abstract fun defineInterface()

    fun start() {
        if (job == null) {
            job = scope.launch {
                processLoop()
            }
        }
    }

    suspend fun send(message: ActorMessage) {
        internalChannel.send(message)
    }

    protected open suspend fun processLoop() {
        for (message in internalChannel) {
            try {
                processMessage(message)
            } catch (e: Exception) {
                handleError(e, message)
            }
        }
    }

    protected abstract suspend fun processMessage(message: ActorMessage)

    protected open fun handleError(error: Exception, message: ActorMessage) {
        println("Error processing message ${message.correlationId}: ${error.message}")
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun getInterface(): ActorInterface = actorInterface
}