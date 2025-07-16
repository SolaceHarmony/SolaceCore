package ai.solace.core.storage

/**
 * File-based implementation of the ConfigurationStorage interface.
 *
 * This implementation stores configuration data in files, with each configuration stored in a separate file.
 * It extends the FileStorage class and implements the ConfigurationStorage interface.
 *
 * @param baseDirectory The base directory where configuration data will be stored.
 */
class FileConfigurationStorage(
    baseDirectory: String
) : FileStorage<String, Map<String, Any>>(
    baseDirectory = baseDirectory,
    keySerializer = { it },
    valueSerializer = { it },
    valueDeserializer = { it }
), ConfigurationStorage {
    /**
     * Gets a configuration value with the given key and path.
     *
     * @param key The key to identify the configuration.
     * @param path The path to the specific configuration value, using dot notation (e.g., "database.url").
     * @return The configuration value, or null if the key or path doesn't exist.
     */
    override suspend fun getConfigValue(key: String, path: String): Any? {
        // If key is empty or path is invalid, return null immediately
        if (key.isEmpty() || !isValidPath(path)) {
            return null
        }

        // Get the config for the key, or return null if it doesn't exist
        val config = retrieve(key)?.first ?: return null

        // Use getValueFromPath to navigate the entire path
        return getValueFromPath(config, path)
    }

    /**
     * Sets a configuration value with the given key and path.
     *
     * @param key The key to identify the configuration.
     * @param path The path to the specific configuration value, using dot notation (e.g., "database.url").
     * @param value The value to set.
     * @return True if the value was set successfully, false otherwise.
     */
    override suspend fun setConfigValue(key: String, path: String, value: Any): Boolean {
        // Validate the path before proceeding
        if (!isValidPath(path)) {
            return false
        }

        // Retrieve the current config
        val retrievedData = retrieve(key)
        val config = retrievedData?.first?.toMutableMap() ?: mutableMapOf()
        val metadata = retrievedData?.second?.toMutableMap() ?: mutableMapOf()

        // Set the value at the specified path
        return try {
            if (!setValueAtPath(config, path, value)) {
                return false
            }

            // Store the updated config
            store(key, config, metadata)
        } catch (e: Exception) {
            // If any exception occurs, return false to avoid hanging
            false
        }
    }

    /**
     * Gets the entire configuration for a component.
     *
     * @param componentId The ID of the component.
     * @return The configuration map for the component, or null if the component doesn't exist.
     */
    override suspend fun getComponentConfig(componentId: String): Map<String, Any>? {
        return retrieve("component:$componentId")?.first
    }

    /**
     * Sets the entire configuration for a component.
     *
     * @param componentId The ID of the component.
     * @param config The configuration map for the component.
     * @return True if the configuration was set successfully, false otherwise.
     */
    override suspend fun setComponentConfig(componentId: String, config: Map<String, Any>): Boolean {
        return store("component:$componentId", config)
    }

    /**
     * Gets the system-wide configuration.
     *
     * @return The system-wide configuration map.
     */
    override suspend fun getSystemConfig(): Map<String, Any> {
        return retrieve("system")?.first ?: emptyMap()
    }

    /**
     * Sets the system-wide configuration.
     *
     * @param config The system-wide configuration map.
     * @return True if the configuration was set successfully, false otherwise.
     */
    override suspend fun setSystemConfig(config: Map<String, Any>): Boolean {
        return store("system", config)
    }

    /**
     * Gets a value from a nested map using a dot-notation path.
     *
     * @param map The map to get the value from.
     * @param path The path to the value, using dot notation (e.g., "database.url").
     * @return The value at the specified path, or null if the path doesn't exist.
     */
    private fun getValueFromPath(map: Map<String, Any>, path: String): Any? {
        // If path is empty, return null
        if (path.isEmpty()) {
            return null
        }

        val parts = path.split(".")
        var current: Any? = map

        for (part in parts) {
            // If current is not a Map, we can't navigate further
            if (current !is Map<*, *>) {
                return null
            }

            // Try to get the value for this part of the path
            @Suppress("UNCHECKED_CAST")
            current = (current as Map<String, Any>)[part]

            // If current becomes null (key not found), return null immediately
            if (current == null) {
                return null
            }
        }

        return current
    }

    /**
     * Sets a value in a nested map using a dot-notation path.
     *
     * @param map The map to set the value in.
     * @param path The path to the value, using dot notation (e.g., "database.url").
     * @param value The value to set.
     * @return True if the value was set successfully, false otherwise.
     */
    private fun setValueAtPath(map: MutableMap<String, Any>, path: String, value: Any): Boolean {
        // Path has already been validated in setConfigValue
        val parts = path.split(".")

        // If there's only one part, set the value directly
        if (parts.size == 1) {
            map[parts[0]] = value
            return true
        }

        var current = map

        // Navigate through the path, creating nested maps as needed
        for (i in 0 until parts.size - 1) {
            val part = parts[i]

            // If the part is empty, return false
            if (part.isEmpty()) {
                return false
            }

            // Get the current value at this path part
            val next = current[part]

            // If the next part exists and is a map, use it
            if (next is MutableMap<*, *>) {
                @Suppress("UNCHECKED_CAST")
                current = next as MutableMap<String, Any>
            } else {
                // Otherwise, create a new map
                val newMap = mutableMapOf<String, Any>()
                current[part] = newMap
                current = newMap
            }
        }

        // Set the value at the last part of the path
        current[parts.last()] = value
        return true
    }

    /**
     * Validates a path for use in configuration operations.
     *
     * @param path The path to validate.
     * @return True if the path is valid, false otherwise.
     */
    private fun isValidPath(path: String): Boolean {
        // Path cannot be empty
        if (path.isEmpty()) {
            return false
        }

        // Path cannot contain consecutive dots
        if (path.contains("..")) {
            return false
        }

        // Path cannot start or end with a dot
        if (path.startsWith(".") || path.endsWith(".")) {
            return false
        }

        // Path cannot contain invalid characters
        val invalidChars = setOf('$', '@', '#', '!', '%', '^', '&', '*', '(', ')', '+', '=', '{', '}', '[', ']', '|', '\\', ':', ';', '"', '\'', '<', '>', ',', '?', '/')
        if (path.any { it in invalidChars }) {
            return false
        }

        return true
    }
}