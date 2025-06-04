package ai.solace.core.storage.encryption

import ai.solace.core.storage.Storage
import ai.solace.core.util.logger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.encodeToString
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
    // Each operation gets its own mutex to avoid deadlocks when operations call each other
    private val storeMutex = Mutex()
    private val retrieveMutex = Mutex()
    private val updateMetadataMutex = Mutex()
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
        try {
            // Prepare data outside of the mutex lock
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

            // Only use the storeMutex for the actual storage operation
            return storeMutex.withLock {
                // Store the encrypted data in the underlying storage
                storage.store(key, encryptedData)
            }
        } catch (e: Exception) {
            logger.error("Failed to store encrypted data for key: $key", e)
            return false
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
        try {
            // First retrieve the encrypted data with a short-lived lock
            val encryptedData = retrieveMutex.withLock {
                try {
                    // Retrieve the encrypted data from the underlying storage
                    storage.retrieve(key)?.first ?: return null
                } catch (e: Exception) {
                    logger.error("Failed to retrieve encrypted data for key: $key", e)
                    return null
                }
            }

            // Safety check for data size before decryption
            if (encryptedData.isEmpty()) {
                logger.warn("Empty encrypted data found for key: $key")
                return null
            }

            // Decrypt the data outside the mutex lock
            val decryptedJson: String
            try {
                decryptedJson = encryptionStrategy.decrypt(encryptedData).toString(Charsets.UTF_8)
            } catch (e: Exception) {
                logger.error("Failed to decrypt data for key: $key", e)
                return null
            }

            // Parse the combined JSON object with safety check
            val jsonElement = try {
                json.parseToJsonElement(decryptedJson)
            } catch (e: Exception) {
                logger.error("Failed to parse JSON for key: $key", e)
                return null
            }
            val combinedObj = jsonElement.jsonObject

            // Extract value and metadata with validation
            val valueElement = combinedObj["value"] ?: run {
                logger.warn("Missing 'value' field in JSON for key: $key")
                return null
            }
            if (valueElement !is JsonPrimitive) {
                logger.warn("'value' field is not a primitive in JSON for key: $key")
                return null
            }
            val valueJson = valueElement.content

            val metadataElement = combinedObj["metadata"] ?: run {
                logger.warn("Missing 'metadata' field in JSON for key: $key")
                return null
            }
            if (metadataElement !is JsonPrimitive) {
                logger.warn("'metadata' field is not a primitive in JSON for key: $key")
                return null
            }
            val metadataJson = metadataElement.content

            // Deserialize value with error handling
            val value = try {
                valueDeserializer(valueJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize value for key: $key", e)
                return null
            }

            // Deserialize metadata with safe parsing
            val metadataJsonElement = try {
                json.parseToJsonElement(metadataJson)
            } catch (e: Exception) {
                logger.error("Failed to parse metadata JSON for key: $key", e)
                return null
            }

            if (metadataJsonElement !is JsonObject) {
                logger.warn("Metadata is not a JSON object for key: $key")
                return null
            }

            val metadataObj = metadataJsonElement
            val metadata = metadataObj.mapValues { (_, element) ->
                when {
                    element is JsonPrimitive -> {
                        val value = when {
                            element.booleanOrNull != null -> element.booleanOrNull
                            element.intOrNull != null -> element.intOrNull
                            element.longOrNull != null -> element.longOrNull
                            element.doubleOrNull != null -> element.doubleOrNull
                            else -> element.content
                        }
                        value ?: element.toString()
                    }
                    else -> element.toString()
                }
            }

            return Pair(value, metadata)
        } catch (e: Exception) {
            logger.error("Failed to retrieve and decrypt data for key: $key", e)
            return null
        }
    }

    /**
     * Lists all keys in the storage.
     *
     * @return A list of all keys.
     */
    override suspend fun listKeys(): List<K> {
        // No mutex needed as we're just delegating to the underlying storage
        return storage.listKeys()
    }

    /**
     * Deletes a value with the given key.
     *
     * @param key The key to identify the value.
     * @return True if the value was deleted successfully, false otherwise.
     */
    override suspend fun delete(key: K): Boolean {
        // We use storeMutex since deletion modifies storage
        return storeMutex.withLock {
            storage.delete(key)
        }
    }

    /**
     * Checks if a key exists in the storage.
     *
     * @param key The key to check.
     * @return True if the key exists, false otherwise.
     */
    override suspend fun exists(key: K): Boolean {
        // No mutex needed for a read-only operation that doesn't interact with our other methods
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
        try {
            // First retrieve the encrypted data with a short-lived lock
            val encryptedData = updateMetadataMutex.withLock {
                storage.retrieve(key) ?: return false
            }

            // Decrypt outside the lock
            val decryptedJson = encryptionStrategy.decrypt(encryptedData.first).toString(Charsets.UTF_8)
            val jsonElement = json.parseToJsonElement(decryptedJson)
            val combinedObj = jsonElement.jsonObject

            val valueElement = combinedObj["value"] ?: return false
            if (valueElement !is JsonPrimitive) return false
            val valueJson = valueElement.content

            // Deserialize value
            val value = try {
                valueDeserializer(valueJson)
            } catch (e: Exception) {
                logger.error("Failed to deserialize value for key: $key", e)
                return false
            }

            // Store the value with the new metadata
            return store(key, value, metadata)
        } catch (e: Exception) {
            logger.error("Failed to update metadata for key: $key", e)
            return false
        }
    }
}
