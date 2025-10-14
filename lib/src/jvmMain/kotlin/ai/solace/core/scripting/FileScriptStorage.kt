package ai.solace.core.scripting

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * A file-based implementation of the ScriptStorage interface.
 *
 * This implementation stores scripts as files in a specified directory.
 * Script metadata is stored as JSON files alongside the script files.
 *
 * @param baseDirectory The base directory where scripts will be stored.
 */
class FileScriptStorage(
    private val baseDirectory: String
) : ScriptStorage {
    /**
     * The directory where scripts will be stored.
     */
    private val scriptDirectory: Path = Paths.get(baseDirectory, "scripts")

    /**
     * The JSON serializer/deserializer.
     */
    private val json = Json { prettyPrint = true }

    init {
        // Create the script directory if it doesn't exist
        Files.createDirectories(scriptDirectory)
    }

    /**
     * Saves a script to storage.
     *
     * @param scriptName The name of the script.
     * @param scriptSource The source code of the script.
     * @param metadata Additional metadata for the script.
     */
    override suspend fun saveScript(scriptName: String, scriptSource: String, metadata: Map<String, Any>) {
        withContext(Dispatchers.IO) {
            // Create the script file
            val scriptFile = scriptDirectory.resolve("$scriptName.kts")
            Files.writeString(scriptFile, scriptSource)

            // Create the metadata file
            val metadataFile = scriptDirectory.resolve("$scriptName.json")
            val metadataJson = createMetadataJson(metadata)
            Files.writeString(metadataFile, metadataJson)
        }
    }

    /**
     * Loads a script from storage.
     *
     * @param scriptName The name of the script.
     * @return The script source code and metadata, or null if the script doesn't exist.
     */
    override suspend fun loadScript(scriptName: String): Pair<String, Map<String, Any>>? {
        return withContext(Dispatchers.IO) {
            // Check if the script file exists
            val scriptFile = scriptDirectory.resolve("$scriptName.kts")
            if (!Files.exists(scriptFile)) {
                return@withContext null
            }

            // Read the script source
            val scriptSource = Files.readString(scriptFile)

            // Read the metadata
            val metadataFile = scriptDirectory.resolve("$scriptName.json")
            val metadata = if (Files.exists(metadataFile)) {
                parseMetadataJson(Files.readString(metadataFile))
            } else {
                emptyMap()
            }

            Pair(scriptSource, metadata)
        }
    }

    /**
     * Lists all available scripts.
     *
     * @return A list of script names.
     */
    override suspend fun listScripts(): List<String> {
        return withContext(Dispatchers.IO) {
            // List all .kts files in the script directory
            Files.list(scriptDirectory)
                .filter { it.toString().endsWith(".kts") }
                .map { it.fileName.toString().removeSuffix(".kts") }
                .toList()
        }
    }

    /**
     * Deletes a script from storage.
     *
     * @param scriptName The name of the script.
     * @return True if the script was deleted, false otherwise.
     */
    override suspend fun deleteScript(scriptName: String): Boolean {
        return withContext(Dispatchers.IO) {
            // Delete the script file
            val scriptFile = scriptDirectory.resolve("$scriptName.kts")
            val scriptDeleted = Files.deleteIfExists(scriptFile)

            // Delete the metadata file
            val metadataFile = scriptDirectory.resolve("$scriptName.json")
            val metadataDeleted = Files.deleteIfExists(metadataFile)

            scriptDeleted || metadataDeleted
        }
    }

    /**
     * Creates a JSON string from a metadata map.
     *
     * @param metadata The metadata map.
     * @return A JSON string representation of the metadata.
     */
    private fun createMetadataJson(metadata: Map<String, Any>): String {
        fun toJsonElement(v: Any?): kotlinx.serialization.json.JsonElement = when (v) {
            null -> JsonPrimitive("null")
            is String -> JsonPrimitive(v)
            is Number -> JsonPrimitive(v)
            is Boolean -> JsonPrimitive(v)
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val m = v as Map<String, Any?>
                JsonObject(m.mapValues { (_, vv) -> toJsonElement(vv) })
            }
            is List<*> -> kotlinx.serialization.json.JsonArray(v.map { toJsonElement(it) })
            else -> JsonPrimitive(v.toString())
        }
        val jsonObject = JsonObject(metadata.mapValues { (_, value) -> toJsonElement(value) })
        return json.encodeToString(JsonObject.serializer(), jsonObject)
    }

    /**
     * Parses a JSON string into a metadata map.
     *
     * @param jsonString The JSON string to parse.
     * @return A map of metadata.
     */
    private fun parseMetadataJson(jsonString: String): Map<String, Any> {
        fun fromJsonElement(el: kotlinx.serialization.json.JsonElement): Any = when (el) {
            is JsonPrimitive -> {
                val content = el.content
                when {
                    el.isString -> content
                    content.equals("true", ignoreCase = true) || content.equals("false", ignoreCase = true) -> content.toBoolean()
                    content.toIntOrNull() != null -> content.toInt()
                    content.toLongOrNull() != null -> content.toLong()
                    content.toDoubleOrNull() != null -> content.toDouble()
                    else -> content
                }
            }
            is kotlinx.serialization.json.JsonArray -> el.map { fromJsonElement(it) }
            is JsonObject -> el.mapValues { (_, v) -> fromJsonElement(v) }
            else -> el.toString()
        }
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        return jsonObject.mapValues { (_, v) -> fromJsonElement(v) }
    }
}
