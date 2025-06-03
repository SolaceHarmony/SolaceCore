package ai.solace.core.scripting

import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.*

class FileScriptVersionManagerTest {

    private lateinit var tempDir: String
    private lateinit var scriptStorage: FileScriptStorage
    private lateinit var versionManager: FileScriptVersionManager

    @BeforeTest
    fun setUp() {
        // Create a temporary directory for testing
        tempDir = Files.createTempDirectory("version-manager-test").toString()
        scriptStorage = FileScriptStorage(tempDir)
        versionManager = FileScriptVersionManager(tempDir, scriptStorage)
    }

    @AfterTest
    fun tearDown() {
        // Delete the temporary directory
        Paths.get(tempDir).toFile().deleteRecursively()
    }

    @Test
    fun testAddVersion() = runBlocking {
        // Define a script
        val scriptName = "test-script"
        val scriptSource = """
            val result = "Hello, World!"
            result
        """.trimIndent()

        // Add a version
        val version = versionManager.addVersion(scriptName, scriptSource)

        // Verify the version number
        assertEquals(1, version)

        // Verify the script was saved
        val loadedScript = scriptStorage.loadScript(scriptName)
        assertNotNull(loadedScript)
        assertEquals(scriptSource, loadedScript.first)
        assertEquals(1, loadedScript.second["version"])
    }

    @Test
    fun testGetVersion() = runBlocking {
        // Define a script
        val scriptName = "test-script"
        val scriptSource1 = """
            val result = "Version 1"
            result
        """.trimIndent()
        val scriptSource2 = """
            val result = "Version 2"
            result
        """.trimIndent()

        // Add two versions
        versionManager.addVersion(scriptName, scriptSource1)
        versionManager.addVersion(scriptName, scriptSource2)

        // Get the first version
        val version1 = versionManager.getVersion(scriptName, 1)
        assertNotNull(version1)
        assertEquals(scriptSource1, version1)

        // Get the second version
        val version2 = versionManager.getVersion(scriptName, 2)
        assertNotNull(version2)
        assertEquals(scriptSource2, version2)

        // Try to get a non-existent version
        val version3 = versionManager.getVersion(scriptName, 3)
        assertNull(version3)
    }

    @Test
    fun testGetLatestVersion() = runBlocking {
        // Define a script
        val scriptName = "test-script"
        val scriptSource1 = """
            val result = "Version 1"
            result
        """.trimIndent()
        val scriptSource2 = """
            val result = "Version 2"
            result
        """.trimIndent()

        // Add two versions
        versionManager.addVersion(scriptName, scriptSource1)
        versionManager.addVersion(scriptName, scriptSource2)

        // Get the latest version
        val latestVersion = versionManager.getLatestVersion(scriptName)
        assertNotNull(latestVersion)
        assertEquals(2, latestVersion.first)
        assertEquals(scriptSource2, latestVersion.second)
    }

    @Test
    fun testRollback() = runBlocking {
        // Define a script
        val scriptName = "test-script"
        val scriptSource1 = """
            val result = "Version 1"
            result
        """.trimIndent()
        val scriptSource2 = """
            val result = "Version 2"
            result
        """.trimIndent()
        val scriptSource3 = """
            val result = "Version 3"
            result
        """.trimIndent()

        // Add three versions
        versionManager.addVersion(scriptName, scriptSource1)
        versionManager.addVersion(scriptName, scriptSource2)
        versionManager.addVersion(scriptName, scriptSource3)

        // Rollback to version 1
        val rollbackSuccess = versionManager.rollback(scriptName, 1)
        assertTrue(rollbackSuccess)

        // Verify the current version is now version 1
        val currentScript = scriptStorage.loadScript(scriptName)
        assertNotNull(currentScript)
        assertEquals(scriptSource1, currentScript.first)
        assertEquals(1, currentScript.second["version"])
        assertTrue(currentScript.second["rollback"] as Boolean)
        assertEquals(3, currentScript.second["previousVersion"])
    }

    @Test
    fun testRollbackToNonExistentVersion() = runBlocking {
        // Define a script
        val scriptName = "test-script"
        val scriptSource = """
            val result = "Hello, World!"
            result
        """.trimIndent()

        // Add a version
        versionManager.addVersion(scriptName, scriptSource)

        // Try to rollback to a non-existent version
        val rollbackSuccess = versionManager.rollback(scriptName, 2)
        assertFalse(rollbackSuccess)

        // Verify the current version is still version 1
        val currentScript = scriptStorage.loadScript(scriptName)
        assertNotNull(currentScript)
        assertEquals(scriptSource, currentScript.first)
        assertEquals(1, currentScript.second["version"])
    }

    @Test
    fun testGetLatestVersionForNonExistentScript() = runBlocking {
        // Try to get the latest version of a non-existent script
        val latestVersion = versionManager.getLatestVersion("non-existent-script")
        assertNull(latestVersion)
    }
}