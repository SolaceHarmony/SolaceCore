package ai.solace.core.storage.serialization

import kotlin.reflect.KClass

/**
 * Registry for actor state serializers.
 *
 * This class manages serializers for different types of actor state objects.
 * It provides methods for registering serializers and retrieving them by type.
 */
class ActorStateSerializerRegistry {
    /**
     * Map of serializers by class type.
     */
    private val serializers = mutableMapOf<KClass<*>, ActorStateSerializer<*>>()

    /**
     * Registers a serializer for a specific type.
     *
     * @param serializer The serializer to register.
     */
    fun <T : Any> registerSerializer(serializer: ActorStateSerializer<T>) {
        serializers[serializer.getType()] = serializer
    }

    /**
     * Gets a serializer for a specific type.
     *
     * @param clazz The class type to get the serializer for.
     * @return The serializer for the specified type, or null if no serializer is registered.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getSerializer(clazz: KClass<T>): ActorStateSerializer<T>? {
        return serializers[clazz] as? ActorStateSerializer<T>
    }

    /**
     * Checks if a serializer is registered for a specific type.
     *
     * @param clazz The class type to check.
     * @return True if a serializer is registered for the specified type, false otherwise.
     */
    fun <T : Any> hasSerializer(clazz: KClass<T>): Boolean {
        return serializers.containsKey(clazz)
    }

    /**
     * Unregisters a serializer for a specific type.
     *
     * @param clazz The class type to unregister the serializer for.
     * @return True if a serializer was unregistered, false otherwise.
     */
    fun <T : Any> unregisterSerializer(clazz: KClass<T>): Boolean {
        return serializers.remove(clazz) != null
    }

    /**
     * Gets all registered serializers.
     *
     * @return A map of class types to serializers.
     */
    fun getAllSerializers(): Map<KClass<*>, ActorStateSerializer<*>> {
        return serializers.toMap()
    }
}