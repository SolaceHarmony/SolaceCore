package ai.solace.core.actor

/**
 * Represents the various states of an actor in the actor system.
 */

sealed class ActorState {
    /**
     * Represents the initial state of an actor in the system.
     *
     * When an actor is in the `Initialized` state, it has been initialized but not yet started.
     */
    object Initialized : ActorState()
    /**
     * Represents the state of an actor when it is actively running.
     * This state indicates that the actor is currently processing messages
     * and performing its designated operations.
     */
    object Running : ActorState()
    /**
     * Represents a state where the Actor is stopped.
     * Can be used to indicate that the Actor has terminated its processing.
     */
    object Stopped : ActorState()
    /**
     * Represents an error state within the actor, containing an exception.
     *
     * @property exception The exception that caused the error state.
     */
    data class Error(val exception: String) : ActorState()
    /**
     * Represents a state where the actor is paused.
     *
     * @property reason The reason why the actor is paused.
     */
    data class Paused(val reason: String) : ActorState()

    /**
     * Returns a string representation of the ActorState.
     *
     * @return a string describing the current state: "Initialized" for Initialized state,
     * "Running" for Running state, "Stopped" for Stopped state, "Error(<message>)" for Error state
     * with the error message, and "Paused(<reason>)" for Paused state with the pause reason.
     */
    override fun toString(): String = when (this) {
        is Initialized -> "Initialized"
        is Running -> "Running"
        is Stopped -> "Stopped"
        is Error -> "Error(${exception})"
        is Paused -> "Paused($reason)"
    }
}