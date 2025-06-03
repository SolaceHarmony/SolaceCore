package ai.solace.core.scripting

/**
 * Manages script versions and rollback capabilities.
 */
interface ScriptVersionManager {
    /**
     * Adds a new version of a script.
     *
     * @param scriptName The name of the script.
     * @param scriptSource The source code of the script.
     * @return The version number of the new script.
     */
    suspend fun addVersion(scriptName: String, scriptSource: String): Int

    /**
     * Gets a specific version of a script.
     *
     * @param scriptName The name of the script.
     * @param version The version number to get.
     * @return The script source code for the specified version, or null if not found.
     */
    suspend fun getVersion(scriptName: String, version: Int): String?

    /**
     * Gets the latest version of a script.
     *
     * @param scriptName The name of the script.
     * @return A pair containing the version number and source code of the latest version,
     *         or null if the script doesn't exist.
     */
    suspend fun getLatestVersion(scriptName: String): Pair<Int, String>?

    /**
     * Rolls back to a previous version of a script.
     *
     * @param scriptName The name of the script.
     * @param version The version to roll back to.
     * @return True if the rollback was successful, false otherwise.
     */
    suspend fun rollback(scriptName: String, version: Int): Boolean
}