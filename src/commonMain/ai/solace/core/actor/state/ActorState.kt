package ai.solace.core.actor.state

/**
 * Enhanced state management for actors with additional states and metadata
 */
sealed class ActorState {
    object Initialized : ActorState()
    object Running : ActorState()
    object Stopped : ActorState()
    data class Error(val exception: Throwable) : ActorState()
    data class Paused(val reason: String) : ActorState()

    override fun toString(): String = when (this) {
        is Initialized -> "Initialized"
        is Running -> "Running"
        is Stopped -> "Stopped"
        is Error -> "Error(${exception.message})"
        is Paused -> "Paused($reason)"
    }
}