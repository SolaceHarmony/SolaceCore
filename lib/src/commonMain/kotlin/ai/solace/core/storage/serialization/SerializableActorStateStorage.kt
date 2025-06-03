package ai.solace.core.storage.serialization

import ai.solace.core.storage.ActorStateStorage
import kotlin.reflect.KClass

/**
 * Extension of ActorStateStorage interface to support serialization.
 *
 * This interface adds methods for serializing and deserializing objects
 * to and from the storage system.
 */
interface SerializableActorStateStorage : ActorStateStorage {
    /**
     * Gets the serializer registry.
     *
     * @return The serializer registry.
     */
    fun getSerializerRegistry(): ActorStateSerializerRegistry

    /**
     * Serializes and stores an object.
     *
     * @param actorId The ID of the actor.
     * @param key The key to identify the object.
     * @param obj The object to serialize and store.
     * @param clazz The class type of the object.
     * @return True if the object was stored successfully, false otherwise.
     */
    suspend fun <T : Any> serializeAndStore(actorId: String, key: String, obj: T, clazz: KClass<T>): Boolean {
        val serializer = getSerializerRegistry().getSerializer(clazz)
            ?: throw IllegalArgumentException("No serializer registered for class ${clazz.simpleName}")
        
        val serialized = serializer.serialize(obj)
        
        // Retrieve the current actor data or create a new map
        val retrievedData = retrieve(actorId)
        val actorData = retrievedData?.first?.toMutableMap() ?: mutableMapOf()
        val metadata = retrievedData?.second?.toMutableMap() ?: mutableMapOf()
        
        // Store the serialized object in the actor data
        val customState = actorData["customState"] as? MutableMap<String, Any> ?: mutableMapOf()
        customState[key] = serialized
        actorData["customState"] = customState
        
        // Store the updated actor data
        return store(actorId, actorData, metadata)
    }

    /**
     * Retrieves and deserializes an object.
     *
     * @param actorId The ID of the actor.
     * @param key The key to identify the object.
     * @param clazz The class type of the object.
     * @return The deserialized object, or null if the object doesn't exist or deserialization failed.
     */
    suspend fun <T : Any> retrieveAndDeserialize(actorId: String, key: String, clazz: KClass<T>): T? {
        val serializer = getSerializerRegistry().getSerializer(clazz)
            ?: throw IllegalArgumentException("No serializer registered for class ${clazz.simpleName}")
        
        // Retrieve the actor data
        val actorData = retrieve(actorId)?.first ?: return null
        
        // Get the custom state map
        val customState = actorData["customState"] as? Map<String, Any> ?: return null
        
        // Get the serialized object
        @Suppress("UNCHECKED_CAST")
        val serialized = customState[key] as? Map<String, Any> ?: return null
        
        // Deserialize the object
        return serializer.deserialize(serialized)
    }
}