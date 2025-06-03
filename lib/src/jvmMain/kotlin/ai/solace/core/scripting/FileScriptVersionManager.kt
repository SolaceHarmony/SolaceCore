package ai.solace.core.scripting

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant

/**
 * A file-based implementation of the ScriptVersionManager interface.
 *
 * This implementation stores script versions as separate files in a specified directory.
 * Each version is stored with a version number in the filename.
 *
 * @param baseDirectory The base directory where script versions will be stored.
 * @param scriptStorage The script storage to use for saving and loading scripts.
 */
class FileScriptVersionManager(
    private val baseDirectory: String,
    private val scriptStorage: ScriptStorage
) : ScriptVersionManager {
    /**
     * The directory where script versions will be stored.
     */
    private val versionsDirectory: Path = Paths.get(baseDirectory, "versions")

    init {
        // Create the versions directory if it doesn't exist
        Files.createDirectories(versionsDirectory)
    }

    /**
     * Adds a new version of a script.
     *
     * @param scriptName The name of the script.
     * @param scriptSource The source code of the script.
     * @return The version number of the new script.
     */
    override suspend fun addVersion(scriptName: String, scriptSource: String): Int {
        return withContext(Dispatchers.IO) {
            // Get the current version number
            val currentVersion = getCurrentVersion(scriptName)
            val newVersion = currentVersion + 1

            // Create the version directory if it doesn't exist
            val scriptVersionsDir = versionsDirectory.resolve(scriptName)
            Files.createDirectories(scriptVersionsDir)

            // Save the new version
            val versionFile = scriptVersionsDir.resolve("$newVersion.kts")
            Files.writeString(versionFile, scriptSource)

            // Update the current version metadata
            val metadata = mapOf(
                "version" to newVersion,
                "timestamp" to Instant.now().toEpochMilli()
            )

            // Save the script with the updated metadata
            scriptStorage.saveScript(scriptName, scriptSource, metadata)

            newVersion
        }
    }

    /**
     * Gets a specific version of a script.
     *
     * @param scriptName The name of the script.
     * @param version The version number to get.
     * @return The script source code for the specified version, or null if not found.
     */
    override suspend fun getVersion(scriptName: String, version: Int): String? {
        return withContext(Dispatchers.IO) {
            val versionFile = versionsDirectory.resolve("$scriptName/$version.kts")
            if (Files.exists(versionFile)) {
                Files.readString(versionFile)
            } else {
                null
            }
        }
    }

    /**
     * Gets the latest version of a script.
     *
     * @param scriptName The name of the script.
     * @return A pair containing the version number and source code of the latest version,
     *         or null if the script doesn't exist.
     */
    override suspend fun getLatestVersion(scriptName: String): Pair<Int, String>? {
        return withContext(Dispatchers.IO) {
            val currentVersion = getCurrentVersion(scriptName)
            if (currentVersion > 0) {
                val scriptData = scriptStorage.loadScript(scriptName)
                if (scriptData != null) {
                    Pair(currentVersion, scriptData.first)
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    /**
     * Rolls back to a previous version of a script.
     *
     * @param scriptName The name of the script.
     * @param version The version to roll back to.
     * @return True if the rollback was successful, false otherwise.
     */
    override suspend fun rollback(scriptName: String, version: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val currentVersion = getCurrentVersion(scriptName)

            if (version <= 0 || version > currentVersion) {
                return@withContext false
            }

            val versionFile = versionsDirectory.resolve("$scriptName/$version.kts")

            if (!Files.exists(versionFile)) {
                return@withContext false
            }

            val scriptSource = Files.readString(versionFile)

            val metadata = mapOf(
                "version" to version,
                "timestamp" to Instant.now().toEpochMilli(),
                "rollback" to true,
                "previousVersion" to currentVersion
            )

            scriptStorage.saveScript(scriptName, scriptSource, metadata)

            true
        }
    }

    /**
     * Gets the current version number of a script by checking both the metadata and the version files.
     * This ensures that we always have the correct version number, even if the metadata is out of sync.
     *
     * @param scriptName The name of the script.
     * @return The current version number, or 0 if the script doesn't exist.
     */
    private suspend fun getCurrentVersion(scriptName: String): Int {
        // Check the metadata for the version
        val scriptData = scriptStorage.loadScript(scriptName)
        val metadataVersion = if (scriptData != null) {
            scriptData.second["version"] as? Int ?: 0
        } else {
            0
        }

        // Also check the versions directory to see if there are any version files
        val scriptVersionsDir = versionsDirectory.resolve(scriptName)
        if (Files.exists(scriptVersionsDir)) {
            val versionFiles = Files.list(scriptVersionsDir)
                .filter { it.toString().endsWith(".kts") }
                .map { it.fileName.toString().removeSuffix(".kts").toIntOrNull() ?: 0 }
                .toList()

            val maxFileVersion = versionFiles.maxOrNull() ?: 0

            // Return the maximum of the metadata version and the version files
            return maxOf(metadataVersion, maxFileVersion)
        }

        return metadataVersion
    }
}
