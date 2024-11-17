package ai.solace.core.actor.interfaces

interface SerializableState {
    fun serialize(): String
}