package ai.solace.core.storage.serialization

import ai.solace.core.actor.ActorState
import kotlin.reflect.KClass

/**
 * Serializer for the ActorState enum.
 *
 * This class provides methods for serializing and deserializing ActorState objects.
 */
class ActorStateEnumSerializer : ActorStateSerializer<ActorState> {
    /**
     * Serializes an ActorState object to a map of key-value pairs.
     *
     * @param obj The ActorState object to serialize.
     * @return A map of key-value pairs representing the serialized ActorState.
     */
    override fun serialize(obj: ActorState): Map<String, Any> {
        return when (obj) {
            is ActorState.Initialized -> mapOf("type" to "Initialized")
            is ActorState.Running -> mapOf("type" to "Running")
            is ActorState.Stopped -> mapOf("type" to "Stopped")
            is ActorState.Error -> mapOf("type" to "Error", "exception" to obj.exception)
            is ActorState.Paused -> mapOf("type" to "Paused", "reason" to obj.reason)
        }
    }

    /**
     * Deserializes a map of key-value pairs to an ActorState object.
     *
     * @param map The map of key-value pairs to deserialize.
     * @return The deserialized ActorState object, or null if deserialization failed.
     */
    override fun deserialize(map: Map<String, Any>): ActorState? {
        return when (map["type"]) {
            "Initialized" -> ActorState.Initialized
            "Running" -> ActorState.Running
            "Stopped" -> ActorState.Stopped
            "Error" -> ActorState.Error(map["exception"] as? String ?: "Unknown error")
            "Paused" -> ActorState.Paused(map["reason"] as? String ?: "Unknown reason")
            else -> null
        }
    }

    /**
     * Gets the class type that this serializer handles.
     *
     * @return The ActorState class.
     */
    override fun getType(): KClass<ActorState> {
        return ActorState::class
    }
}