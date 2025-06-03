package ai.solace.core.scripting

import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.*

class FileScriptStorageTest {

    private lateinit var tempDir: String
    private lateinit var scriptStorage: FileScriptStorage

    @BeforeTest
    fun setUp() {
        // Create a temporary directory for testing
        tempDir = Files.createTempDirectory("script-storage-test").toString()
        scriptStorage = FileScriptStorage(tempDir)
    }

    @AfterTest
    fun tearDown() {
        // Delete the temporary directory
        Paths.get(tempDir).toFile().deleteRecursively()
    }

    @Test
    fun testSaveAndLoadScript() = runBlocking {
        // Define a script
        val scriptName = "test-script"
        val scriptSource = """
            val result = "Hello, World!"
            result
        """.trimIndent()
        val metadata = mapOf(
            "author" to "Test Author",
            "version" to 1,
            "description" to "A test script"
        )

        // Save the script
        scriptStorage.saveScript(scriptName, scriptSource, metadata)

        // Load the script
        val loadedScript = scriptStorage.loadScript(scriptName)

        // Verify the loaded script
        assertNotNull(loadedScript)
        assertEquals(scriptSource, loadedScript.first)
        assertEquals(metadata["author"], loadedScript.second["author"])
        assertEquals(metadata["version"], loadedScript.second["version"])
        assertEquals(metadata["description"], loadedScript.second["description"])
    }

    @Test
    fun testListScripts() = runBlocking {
        // Save multiple scripts
        val script1 = "script1"
        val script2 = "script2"
        val script3 = "script3"
        val scriptSource = "val result = 42"

        scriptStorage.saveScript(script1, scriptSource, emptyMap())
        scriptStorage.saveScript(script2, scriptSource, emptyMap())
        scriptStorage.saveScript(script3, scriptSource, emptyMap())

        // List the scripts
        val scripts = scriptStorage.listScripts()

        // Verify the list
        assertEquals(3, scripts.size)
        assertTrue(scripts.contains(script1))
        assertTrue(scripts.contains(script2))
        assertTrue(scripts.contains(script3))
    }

    @Test
    fun testDeleteScript() = runBlocking {
        // Save a script
        val scriptName = "script-to-delete"
        val scriptSource = "val result = 42"

        scriptStorage.saveScript(scriptName, scriptSource, emptyMap())

        // Verify the script exists
        val scripts = scriptStorage.listScripts()
        assertTrue(scripts.contains(scriptName))

        // Delete the script
        val deleted = scriptStorage.deleteScript(scriptName)

        // Verify the script was deleted
        assertTrue(deleted)
        val scriptsAfterDelete = scriptStorage.listScripts()
        assertFalse(scriptsAfterDelete.contains(scriptName))
    }

    @Test
    fun testLoadNonExistentScript() = runBlocking {
        // Try to load a script that doesn't exist
        val loadedScript = scriptStorage.loadScript("non-existent-script")

        // Verify the result is null
        assertNull(loadedScript)
    }

    @Test
    fun testDeleteNonExistentScript() = runBlocking {
        // Try to delete a script that doesn't exist
        val deleted = scriptStorage.deleteScript("non-existent-script")

        // Verify the result is false
        assertFalse(deleted)
    }
}