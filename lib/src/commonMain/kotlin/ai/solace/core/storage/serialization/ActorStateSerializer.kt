package ai.solace.core.storage.serialization

import kotlin.reflect.KClass

/**
 * Interface for serializing and deserializing actor state.
 *
 * This interface defines methods for converting objects to and from a map representation
 * that can be stored in various storage backends.
 *
 * @param T The type of object to serialize/deserialize.
 */
interface ActorStateSerializer<T : Any> {
    /**
     * Serializes an object to a map of key-value pairs.
     *
     * @param obj The object to serialize.
     * @return A map of key-value pairs representing the serialized object.
     */
    fun serialize(obj: T): Map<String, Any>

    /**
     * Deserializes a map of key-value pairs to an object.
     *
     * @param map The map of key-value pairs to deserialize.
     * @return The deserialized object, or null if deserialization failed.
     */
    fun deserialize(map: Map<String, Any>): T?
    
    /**
     * Gets the class type that this serializer handles.
     *
     * @return The class type.
     */
    fun getType(): KClass<T>
}