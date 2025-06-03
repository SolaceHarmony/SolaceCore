package ai.solace.core.scripting

/**
 * Handles persistent storage of scripts.
 */
interface ScriptStorage {
    /**
     * Saves a script to storage.
     *
     * @param scriptName The name of the script.
     * @param scriptSource The source code of the script.
     * @param metadata Additional metadata for the script.
     */
    suspend fun saveScript(scriptName: String, scriptSource: String, metadata: Map<String, Any> = emptyMap())

    /**
     * Loads a script from storage.
     *
     * @param scriptName The name of the script.
     * @return The script source code and metadata, or null if the script doesn't exist.
     */
    suspend fun loadScript(scriptName: String): Pair<String, Map<String, Any>>?

    /**
     * Lists all available scripts.
     *
     * @return A list of script names.
     */
    suspend fun listScripts(): List<String>

    /**
     * Deletes a script from storage.
     *
     * @param scriptName The name of the script.
     * @return True if the script was deleted, false otherwise.
     */
    suspend fun deleteScript(scriptName: String): Boolean
}