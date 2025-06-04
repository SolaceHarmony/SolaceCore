package ai.solace.core.storage.compression

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable

/**
 * Interface for compression strategies.
 *
 * This interface defines the contract for compression strategies, which determine
 * how data is compressed and decompressed.
 *
 * Note: Due to type erasure in Java/Kotlin, the deserialize method requires a Class object
 * to determine the type to deserialize to. This is a limitation of the JVM.
 */
interface CompressionStrategy {
    /**
     * Compresses the given data.
     *
     * @param data The data to compress.
     * @return The compressed data.
     */
    fun compress(data: ByteArray): ByteArray

    /**
     * Decompresses the given data.
     *
     * @param data The data to decompress.
     * @return The decompressed data.
     */
    fun decompress(data: ByteArray): ByteArray

    /**
     * Serializes an object to a byte array.
     *
     * @param value The object to serialize.
     * @return The serialized data.
     */
    fun serialize(value: Any): ByteArray

    /**
     * Deserializes a byte array to an object.
     *
     * @param data The data to deserialize.
     * @param clazz The class of the object to deserialize to.
     * @return The deserialized object.
     */
    fun <T> deserialize(data: ByteArray, clazz: Class<T>): T
}

/**
 * A simple serialization wrapper for non-serializable types.
 *
 * @param value The value to wrap.
 */
@Serializable
data class SerializationWrapper(val value: String)
