package ai.solace.core.storage.encryption

import ai.solace.core.storage.Storage
import kotlinx.serialization.json.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A storage wrapper that encrypts data before storing it.
 *
 * This class wraps another storage implementation and provides encryption for the values
 * stored in it. The keys are not encrypted, only the values and metadata.
 *
 * @param K The type of the key used to identify the data.
 * @param V The type of the value to be stored.
 * @param storage The underlying storage implementation.
 * @param encryptionStrategy The encryption strategy to use.
 * @param valueSerializer A function that converts a value to a JSON string for encryption.
 * @param valueDeserializer A function that converts a JSON string back to a value after decryption.
 */
class EncryptedStorage<K, V>(
    private val storage: Storage<K, ByteArray>,
    private val encryptionStrategy: EncryptionStrategy,
    private val valueSerializer: (V) -> String = { Json.encodeToString(JsonElement.serializer(), Json.parseToJsonElement(it.toString())) },
    private val valueDeserializer: (String) -> V = { 
        @Suppress("UNCHECKED_CAST")
        Json.parseToJsonElement(it) as V 
    }
) : Storage<K, V> {
    private val mutex = Mutex()
    private val json = Json { prettyPrint = true }

    /**
     * Stores a value with the given key.
     *
     * The value and metadata are serialized to JSON, encrypted, and then stored
     * in the underlying storage.
     *
     * @param key The key to identify the value.
     * @param value The value to store.
     * @param metadata Additional metadata for the value.
     * @return True if the value was stored successfully, false otherwise.
     */
    override suspend fun store(key: K, value: V, metadata: Map<String, Any>): Boolean {
        return mutex.withLock {
            try {
                // Serialize value to JSON
                val valueJson = valueSerializer(value)

                // Serialize metadata to JSON
                val metadataJson = json.encodeToString(JsonObject.serializer(), JsonObject(metadata.mapValues {
                    when (val v = it.value) {
                        is String -> JsonPrimitive(v)
                        is Number -> JsonPrimitive(v)
                        is Boolean -> JsonPrimitive(v)
                        else -> JsonPrimitive(v.toString())
                    }
                }))

                // Create a combined JSON object with value and metadata
                val combinedJson = json.encodeToString(JsonObject.serializer(), JsonObject(mapOf(
                    "value" to JsonPrimitive(valueJson),
                    "metadata" to JsonPrimitive(metadataJson)
                )))

                // Encrypt the combined JSON
                val encryptedData = encryptionStrategy.encrypt(combinedJson.toByteArray(Charsets.UTF_8))

                // Store the encrypted data in the underlying storage
                storage.store(key, encryptedData)
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Retrieves a value with the given key.
     *
     * The encrypted data is retrieved from the underlying storage, decrypted, and then
     * deserialized to extract the value and metadata.
     *
     * @param key The key to identify the value.
     * @return The value and its metadata, or null if the key doesn't exist.
     */
    override suspend fun retrieve(key: K): Pair<V, Map<String, Any>>? {
        return mutex.withLock {
            try {
                // Retrieve the encrypted data from the underlying storage
                val encryptedData = storage.retrieve(key)?.first ?: return@withLock null

                // Decrypt the data
                val decryptedJson = encryptionStrategy.decrypt(encryptedData).toString(Charsets.UTF_8)

                // Parse the combined JSON object
                val combinedObj = json.parseToJsonElement(decryptedJson).jsonObject

                // Extract value and metadata
                val valueJson = combinedObj["value"]?.jsonPrimitive?.content ?: return@withLock null
                val metadataJson = combinedObj["metadata"]?.jsonPrimitive?.content ?: return@withLock null

                // Deserialize value
                val value = valueDeserializer(valueJson)

                // Deserialize metadata
                val metadataObj = json.parseToJsonElement(metadataJson).jsonObject
                val metadata = metadataObj.mapValues { (_, element) ->
                    when {
                        element is JsonPrimitive && element.isString -> element.content
                        element is JsonPrimitive && element.content == "true" -> true
                        element is JsonPrimitive && element.content == "false" -> false
                        element is JsonPrimitive && element.content.toIntOrNull() != null -> element.content.toInt()
                        element is JsonPrimitive && element.content.toLongOrNull() != null -> element.content.toLong()
                        element is JsonPrimitive && element.content.toDoubleOrNull() != null -> element.content.toDouble()
                        else -> element.toString()
                    }
                }

                Pair(value, metadata)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Lists all keys in the storage.
     *
     * @return A list of all keys.
     */
    override suspend fun listKeys(): List<K> {
        return storage.listKeys()
    }

    /**
     * Deletes a value with the given key.
     *
     * @param key The key to identify the value.
     * @return True if the value was deleted successfully, false otherwise.
     */
    override suspend fun delete(key: K): Boolean {
        return storage.delete(key)
    }

    /**
     * Checks if a key exists in the storage.
     *
     * @param key The key to check.
     * @return True if the key exists, false otherwise.
     */
    override suspend fun exists(key: K): Boolean {
        return storage.exists(key)
    }

    /**
     * Updates the metadata for a value with the given key.
     *
     * The current value and metadata are retrieved, the metadata is updated,
     * and then the value and updated metadata are stored back.
     *
     * @param key The key to identify the value.
     * @param metadata The new metadata to set.
     * @return True if the metadata was updated successfully, false otherwise.
     */
    override suspend fun updateMetadata(key: K, metadata: Map<String, Any>): Boolean {
        return mutex.withLock {
            try {
                // Retrieve the current value and metadata
                val current = retrieve(key) ?: return@withLock false

                // Store the value with the new metadata
                store(key, current.first, metadata)
            } catch (e: Exception) {
                false
            }
        }
    }
}
