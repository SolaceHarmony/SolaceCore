package ai.solace.core.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

/**
 * File-based implementation of the Storage interface.
 *
 * This implementation stores data in files, with each key-value pair stored in a separate file.
 * The key is used to generate the filename, and the value is serialized to JSON and stored in the file.
 * Metadata is stored in a separate JSON file alongside the value file.
 *
 * @param K The type of the key used to identify the data.
 * @param V The type of the value to be stored.
 * @param baseDirectory The base directory where data will be stored.
 * @param keySerializer A function that converts a key to a string for use in filenames.
 * @param valueSerializer A function that converts a value to a map for serialization.
 * @param valueDeserializer A function that converts a map back to a value after deserialization.
 */
open class FileStorage<K, V>(
    private val baseDirectory: String,
    private val keySerializer: (K) -> String = { it.toString() },
    private val valueSerializer: (V) -> Map<String, Any> = { value -> 
        if (value is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            (value as Map<String, Any>)
        } else {
            mapOf("value" to (value as Any))
        }
    },
    private val valueDeserializer: (Map<String, Any>) -> V = { 
        @Suppress("UNCHECKED_CAST")
        if (it.containsKey("value") && it.size == 1) {
            it["value"] as V
        } else {
            it as V
        }
    }
) : Storage<K, V> {
    /**
     * The directory where data will be stored.
     */
    private val storageDirectory: Path = Paths.get(baseDirectory, "storage")

    /**
     * The JSON serializer/deserializer.
     */
    private val json = Json { prettyPrint = true }

    /**
     * Mutex for thread-safe access to the storage.
     */
    protected val mutex = Mutex()

    /**
     * Cache of loaded values to improve performance.
     */
    private val cache = ConcurrentHashMap<K, Pair<V, Map<String, Any>>>()

    init {
        // Create the storage directory if it doesn't exist
        Files.createDirectories(storageDirectory)
    }

    /**
     * Stores a value with the given key.
     *
     * @param key The key to identify the value.
     * @param value The value to store.
     * @param metadata Additional metadata for the value.
     * @return True if the value was stored successfully, false otherwise.
     */
    override suspend fun store(key: K, value: V, metadata: Map<String, Any>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val keyString = keySerializer(key)
                val valueMap = valueSerializer(value)

                // Create the value file
                val valueFile = storageDirectory.resolve("$keyString.json")
                val valueJson = createJson(valueMap)
                Files.writeString(valueFile, valueJson)

                // Create the metadata file
                val metadataFile = storageDirectory.resolve("$keyString.metadata.json")
                val metadataJson = createJson(metadata)
                Files.writeString(metadataFile, metadataJson)

                // Update the cache
                mutex.withLock {
                    cache[key] = Pair(value, metadata)
                }

                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Retrieves a value with the given key.
     *
     * @param key The key to identify the value.
     * @return The value and its metadata, or null if the key doesn't exist.
     */
    override suspend fun retrieve(key: K): Pair<V, Map<String, Any>>? {
        return withContext(Dispatchers.IO) {
            try {
                // Check the cache first
                mutex.withLock {
                    cache[key]?.let { return@withContext it }
                }

                val keyString = keySerializer(key)

                // Check if the value file exists
                val valueFile = storageDirectory.resolve("$keyString.json")
                if (!Files.exists(valueFile)) {
                    return@withContext null
                }

                // Read the value
                val valueJson = Files.readString(valueFile)
                val valueMap = parseJson(valueJson)
                val value = valueDeserializer(valueMap)

                // Read the metadata
                val metadataFile = storageDirectory.resolve("$keyString.metadata.json")
                val metadata = if (Files.exists(metadataFile)) {
                    parseJson(Files.readString(metadataFile))
                } else {
                    emptyMap()
                }

                // Update the cache
                val result = Pair(value, metadata)
                mutex.withLock {
                    cache[key] = result
                }

                result
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
        return withContext(Dispatchers.IO) {
            try {
                // Get all key strings from files
                val keyStrings = Files.list(storageDirectory)
                    .filter { it.toString().endsWith(".json") && !it.toString().endsWith(".metadata.json") }
                    .map { it.fileName.toString().removeSuffix(".json") }
                    .toList()

                // Get all keys from cache
                val cachedKeys = mutex.withLock {
                    cache.keys.toList()
                }

                // Combine keys from files and cache
                val result = mutableListOf<K>()

                // Add keys from cache
                result.addAll(cachedKeys)

                // Add keys from files that aren't in cache
                for (keyString in keyStrings) {
                    val cachedKey = cachedKeys.find { keySerializer(it) == keyString }
                    if (cachedKey == null) {
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val key = keyString as K
                            result.add(key)
                        } catch (e: Exception) {
                            // Skip keys that can't be deserialized
                        }
                    }
                }

                result
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Deletes a value with the given key.
     *
     * @param key The key to identify the value.
     * @return True if the value was deleted successfully, false otherwise.
     */
    override suspend fun delete(key: K): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val keyString = keySerializer(key)

                // Delete the value file
                val valueFile = storageDirectory.resolve("$keyString.json")
                val valueDeleted = Files.deleteIfExists(valueFile)

                // Delete the metadata file
                val metadataFile = storageDirectory.resolve("$keyString.metadata.json")
                val metadataDeleted = Files.deleteIfExists(metadataFile)

                // Remove from the cache
                mutex.withLock {
                    cache.remove(key)
                }

                valueDeleted || metadataDeleted
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Checks if a key exists in the storage.
     *
     * @param key The key to check.
     * @return True if the key exists, false otherwise.
     */
    override suspend fun exists(key: K): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Check the cache first
                mutex.withLock {
                    if (cache.containsKey(key)) {
                        return@withContext true
                    }
                }

                val keyString = keySerializer(key)
                val valueFile = storageDirectory.resolve("$keyString.json")
                Files.exists(valueFile)
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Updates the metadata for a value with the given key.
     *
     * @param key The key to identify the value.
     * @param metadata The new metadata to set.
     * @return True if the metadata was updated successfully, false otherwise.
     */
    override suspend fun updateMetadata(key: K, metadata: Map<String, Any>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val keyString = keySerializer(key)

                // Check if the value file exists
                val valueFile = storageDirectory.resolve("$keyString.json")
                if (!Files.exists(valueFile)) {
                    return@withContext false
                }

                // Create the metadata file
                val metadataFile = storageDirectory.resolve("$keyString.metadata.json")
                val metadataJson = createJson(metadata)
                Files.writeString(metadataFile, metadataJson)

                // Update the cache
                mutex.withLock {
                    cache[key]?.let { (value, _) ->
                        cache[key] = Pair(value, metadata)
                    }
                }

                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Clears the cache to force reloading from disk.
     */
    suspend fun clearCache() {
        mutex.withLock {
            cache.clear()
        }
    }

    /**
     * Creates a JSON string from a map.
     *
     * @param map The map to convert to JSON.
     * @return A JSON string representation of the map.
     */
    protected fun createJson(map: Map<String, Any>): String {
        val jsonObject = JsonObject(map.mapValues { (_, value) ->
            when (value) {
                is String -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val nestedMap = value as Map<String, Any>
                    json.parseToJsonElement(createJson(nestedMap))
                }
                is List<*> -> {
                    // Convert list to string for now
                    JsonPrimitive(value.toString())
                }
                else -> JsonPrimitive(value.toString())
            }
        })
        return json.encodeToString(JsonObject.serializer(), jsonObject)
    }

    /**
     * Parses a JSON string into a map.
     *
     * @param jsonString The JSON string to parse.
     * @return A map representation of the JSON.
     */
    protected fun parseJson(jsonString: String): Map<String, Any> {
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        return jsonObject.mapValues { (_, value) ->
            when (value) {
                is JsonPrimitive -> {
                    when {
                        value.isString -> value.content
                        value.content == "true" || value.content == "false" -> value.content.toBoolean()
                        value.content.toIntOrNull() != null -> value.content.toInt()
                        value.content.toLongOrNull() != null -> value.content.toLong()
                        value.content.toDoubleOrNull() != null -> value.content.toDouble()
                        else -> value.content
                    }
                }
                is JsonObject -> {
                    // Recursively parse nested JSON objects
                    parseJson(value.toString())
                }
                else -> value.toString()
            }
        }
    }
}
