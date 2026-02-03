package org.solace.composeapp.actor

/**
 * Stub actor state classes for UI demonstration
 * These mirror the actual actor system states for UI purposes
 */
sealed class ActorState {
    object Initialized : ActorState()
    object Running : ActorState()
    object Stopped : ActorState()
    data class Error(val exception: String) : ActorState()
    data class Paused(val reason: String) : ActorState()
    
    override fun toString(): String = when (this) {
        is Initialized -> "Initialized"
        is Running -> "Running"
        is Stopped -> "Stopped"
        is Error -> "Error($exception)"
        is Paused -> "Paused($reason)"
    }
}