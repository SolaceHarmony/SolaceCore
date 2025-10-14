package ai.solace.core.storage

import kotlin.reflect.KClass

/**
 * Interface for serializing and deserializing objects for storage.
 *
 * This interface defines methods for converting objects to and from a serialized format
 * that can be stored in a storage system. Implementations of this interface should handle
 * the serialization and deserialization of specific types of objects.
 *
 * @param T The type of object to serialize and deserialize.
 */
interface StorageSerializer<T> {
    /**
     * Serializes an object to a map that can be stored in a storage system.
     *
     * @param obj The object to serialize.
     * @return A map representation of the object.
     */
    fun serialize(obj: T): Map<String, Any>

    /**
     * Deserializes a map from a storage system to an object.
     *
     * @param map The map representation of the object.
     * @return The deserialized object.
     */
    fun deserialize(map: Map<String, Any>): T
}

/**
 * Utility class for serializing and deserializing objects for storage.
 *
 * This class provides static methods for registering and using serializers for different types of objects.
 */
object StorageSerializerRegistry {
    /**
     * Map of class names to serializers.
     */
    private val serializers = mutableMapOf<String, StorageSerializer<*>>()

    /**
     * Registers a serializer for a specific type.
     *
     * @param clazz The class of the type to register the serializer for.
     * @param serializer The serializer to register.
     */
    fun <T : Any> registerSerializer(clazz: KClass<T>, serializer: StorageSerializer<T>) {
        val key = clazz.qualifiedName ?: clazz.toString()
        serializers[key] = serializer
    }

    /**
     * Gets a serializer for a specific type.
     *
     * @param clazz The class of the type to get the serializer for.
     * @return The serializer for the specified type, or null if no serializer is registered.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getSerializer(clazz: KClass<T>): StorageSerializer<T>? {
        val key = clazz.qualifiedName ?: clazz.toString()
        return serializers[key] as? StorageSerializer<T>
    }

    /**
     * Serializes an object using the registered serializer for its type.
     *
     * @param obj The object to serialize.
     * @return A map representation of the object, or null if no serializer is registered for the object's type.
     */
    fun serialize(obj: Any): Map<String, Any>? {
        @Suppress("UNCHECKED_CAST")
        val serializer = getSerializer(obj::class as KClass<Any>) ?: return null
        return serializer.serialize(obj)
    }

    /**
     * Deserializes a map to an object using the registered serializer for the specified type.
     *
     * @param map The map representation of the object.
     * @param clazz The class of the type to deserialize to.
     * @return The deserialized object, or null if no serializer is registered for the specified type.
     */
    fun <T : Any> deserialize(map: Map<String, Any>, clazz: KClass<T>): T? {
        val serializer = getSerializer(clazz) ?: return null
        return serializer.deserialize(map)
    }
}
