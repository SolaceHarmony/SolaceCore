package ai.solace.core.storage.compression

import ai.solace.core.storage.serialization.SerializationWrapper
import ai.solace.core.util.logger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Implementation of the CompressionStrategy interface using GZIP compression.
 *
 * This implementation uses GZIP compression to compress and decompress data.
 * It also provides serialization and deserialization using JSON.
 */
class GZIPCompressionStrategy : CompressionStrategy {
    /**
     * The JSON serializer/deserializer.
     */
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }

    /**
     * Compresses the given data using GZIP.
     *
     * @param data The data to compress.
     * @return The compressed data, or the original data if compression would make it larger.
     */
    override fun compress(data: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use { gzipOutputStream ->
            gzipOutputStream.write(data)
        }
        val compressed = outputStream.toByteArray()

        // Return the compressed data only if it's actually smaller
        return if (compressed.size < data.size) {
            compressed
        } else {
            // If compression doesn't help, return the original data
            data
        }
    }

    /**
     * Decompresses the given data using GZIP.
     *
     * @param data The data to decompress.
     * @return The decompressed data.
     */
    override fun decompress(data: ByteArray): ByteArray {
        val inputStream = ByteArrayInputStream(data)
        val outputStream = ByteArrayOutputStream()

        GZIPInputStream(inputStream).use { gzipInputStream ->
            val buffer = ByteArray(1024)
            var len: Int
            while (gzipInputStream.read(buffer).also { len = it } > 0) {
                outputStream.write(buffer, 0, len)
            }
        }

        return outputStream.toByteArray()
    }

    /**
     * Serializes an object to a byte array using JSON.
     *
     * @param value The object to serialize.
     * @return The serialized data.
     */
    override fun serialize(value: Any): ByteArray {
        return when (value) {
            is ByteArray -> value
            is String -> value.toByteArray()
            is Int, is Long, is Float, is Double, is Boolean -> value.toString().toByteArray()
            is Map<*, *> -> {
                // Special handling for maps to ensure they're serialized correctly
                val jsonObject = buildJsonObject {
                    for ((k, v) in value) {
                        val key = k.toString()
                        when (v) {
                            is String -> put(key, v)
                            is Int -> put(key, v)
                            is Long -> put(key, v)
                            is Float -> put(key, v)
                            is Double -> put(key, v)
                            is Boolean -> put(key, v)
                            else -> put(key, v.toString())
                        }
                    }
                }
                json.encodeToString(JsonObject.serializer(), jsonObject).toByteArray()
            }
            else -> {
                try {
                    // Try to serialize directly
                    json.encodeToString(value).toByteArray()
                } catch (e: Exception) {
                    logger.error("Failed to serialize object of type: ${value.javaClass.name}", e)

                    // Fall back to string representation
                    val wrapper = SerializationWrapper(value.toString())
                    json.encodeToString(wrapper).toByteArray()
                }
            }
        }
    }

    /**
     * Deserializes a byte array to an object using JSON.
     *
     * @param data The data to deserialize.
     * @param clazz The class of the object to deserialize to.
     * @return The deserialized object.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> deserialize(data: ByteArray, clazz: Class<T>): T {
        return when {
            clazz == ByteArray::class.java -> data as T
            clazz == String::class.java -> String(data) as T
            clazz == Int::class.java -> String(data).toInt() as T
            clazz == Long::class.java -> String(data).toLong() as T
            clazz == Float::class.java -> String(data).toFloat() as T
            clazz == Double::class.java -> String(data).toDouble() as T
            clazz == Boolean::class.java -> String(data).toBoolean() as T
            else -> {
                try {
                    // For known serializable types, use reflection to find the appropriate method
                    val jsonString = String(data)

                    // Handle Map types specially
                    if (clazz.isAssignableFrom(Map::class.java)) {
                        // Parse the JSON string manually to create a Map
                        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
                        val map = mutableMapOf<String, Any>()

                        for ((key, element) in jsonObject) {
                            val value = when (element) {
                                is JsonPrimitive -> {
                                    when {
                                        element.booleanOrNull != null -> element.booleanOrNull
                                        element.doubleOrNull != null -> element.doubleOrNull
                                        element.longOrNull != null -> element.longOrNull
                                        element.intOrNull != null -> element.intOrNull
                                        else -> element.toString().removeSurrounding("\"")
                                    }
                                }
                                else -> element.toString()
                            }
                            map[key] = value ?: element.toString()
                        }

                        return map as T
                    } else {
                        // Fall back to string representation
                        val wrapper = json.decodeFromString<SerializationWrapper>(jsonString)
                        wrapper.value as T
                    }
                } catch (e: Exception) {
                    // Last resort: return the string itself if it's compatible
                    if (clazz.isAssignableFrom(String::class.java)) {
                        String(data) as T
                    } else {
                        throw IllegalArgumentException("Cannot deserialize to type ${clazz.name}", e)
                    }
                }
            }
        }
    }
}
