package ai.solace.core.storage

/**
 * Interface for storing and retrieving configuration data.
 *
 * This interface extends the generic Storage interface with configuration-specific functionality.
 * It is designed to handle configuration data for the system and its components.
 */
interface ConfigurationStorage : Storage<String, Map<String, Any>> {
    /**
     * Gets a configuration value with the given key and path.
     *
     * @param key The key to identify the configuration.
     * @param path The path to the specific configuration value, using dot notation (e.g., "database.url").
     * @return The configuration value, or null if the key or path doesn't exist.
     */
    suspend fun getConfigValue(key: String, path: String): Any?

    /**
     * Sets a configuration value with the given key and path.
     *
     * @param key The key to identify the configuration.
     * @param path The path to the specific configuration value, using dot notation (e.g., "database.url").
     * @param value The value to set.
     * @return True if the value was set successfully, false otherwise.
     */
    suspend fun setConfigValue(key: String, path: String, value: Any): Boolean

    /**
     * Gets the entire configuration for a component.
     *
     * @param componentId The ID of the component.
     * @return The configuration map for the component, or null if the component doesn't exist.
     */
    suspend fun getComponentConfig(componentId: String): Map<String, Any>?

    /**
     * Sets the entire configuration for a component.
     *
     * @param componentId The ID of the component.
     * @param config The configuration map for the component.
     * @return True if the configuration was set successfully, false otherwise.
     */
    suspend fun setComponentConfig(componentId: String, config: Map<String, Any>): Boolean

    /**
     * Gets the system-wide configuration.
     *
     * @return The system-wide configuration map.
     */
    suspend fun getSystemConfig(): Map<String, Any>

    /**
     * Sets the system-wide configuration.
     *
     * @param config The system-wide configuration map.
     * @return True if the configuration was set successfully, false otherwise.
     */
    suspend fun setSystemConfig(config: Map<String, Any>): Boolean
}