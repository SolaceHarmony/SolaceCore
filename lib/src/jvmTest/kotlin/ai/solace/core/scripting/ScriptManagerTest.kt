package ai.solace.core.scripting

import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.*

class ScriptManagerTest {

    private lateinit var tempDir: String
    private lateinit var scriptEngine: JvmScriptEngine
    private lateinit var scriptStorage: FileScriptStorage
    private lateinit var scriptVersionManager: FileScriptVersionManager
    private lateinit var scriptValidator: SimpleScriptValidator
    private lateinit var scriptManager: ScriptManager

    @BeforeTest
    fun setup() {
        // Create a temporary directory for script storage
        tempDir = Files.createTempDirectory("script-manager-test").toString()

        // Create the components
        scriptEngine = JvmScriptEngine()
        scriptStorage = FileScriptStorage(tempDir)
        scriptVersionManager = FileScriptVersionManager(tempDir, scriptStorage)
        scriptValidator = SimpleScriptValidator()

        // Create the script manager
        scriptManager = ScriptManager(
            scriptEngine = scriptEngine,
            scriptStorage = scriptStorage,
            scriptVersionManager = scriptVersionManager,
            scriptValidator = scriptValidator
        )
    }

    @AfterTest
    fun teardown() {
        // Delete the temporary directory
        Paths.get(tempDir).toFile().deleteRecursively()
    }

    @Test
    fun testCompileAndSave() = runBlocking {
        // Define a simple script
        val scriptName = "test-script"
        val scriptSource = """
            val result = "Hello, World!"
            result
        """.trimIndent()

        // Compile and save the script
        val compiledScript = scriptManager.compileAndSave(scriptName, scriptSource)

        // Verify the compiled script
        assertNotNull(compiledScript)
        assertEquals(scriptName, compiledScript.name)

        // Verify that the script was saved
        val savedScript = scriptStorage.loadScript(scriptName)
        assertNotNull(savedScript)
        assertEquals(scriptSource, savedScript.first)
    }

    @Test
    fun testLoadAndCompile() = runBlocking {
        // Define a simple script
        val scriptName = "test-script"
        val scriptSource = """
            val result = "Hello, World!"
            result
        """.trimIndent()

        // Save the script directly to storage
        scriptStorage.saveScript(scriptName, scriptSource)

        // Load and compile the script
        val compiledScript = scriptManager.loadAndCompile(scriptName)

        // Verify the compiled script
        assertNotNull(compiledScript)
        assertEquals(scriptName, compiledScript.name)
    }

    @Test
    fun testExecute() = runBlocking {
        // Define a simple script that uses a parameter
        val scriptName = "greeting-script"
        val scriptSource = """
            val greeting = "Hello, " + name + "!"
            greeting
        """.trimIndent()

        // Compile and save the script
        scriptManager.compileAndSave(scriptName, scriptSource)

        // Execute the script with parameters
        val result = scriptManager.execute(scriptName, mapOf("name" to "World"))

        // Verify the result
        assertEquals("Hello, World!", result)
    }

    @Test
    fun testReloadScript() = runBlocking {
        // Define a simple script
        val scriptName = "test-script"
        val scriptSource = """
            val result = "Hello, World!"
            result
        """.trimIndent()

        // Compile and save the script
        scriptManager.compileAndSave(scriptName, scriptSource)

        // Define an updated script
        val updatedScriptSource = """
            val result = "Hello, Updated World!"
            result
        """.trimIndent()

        // Save the updated script directly to storage
        scriptStorage.saveScript(scriptName, updatedScriptSource)

        // Execute the script without reloading (should use cached version)
        val resultBeforeReload = scriptManager.execute(scriptName)
        assertEquals("Hello, World!", resultBeforeReload)

        // Reload the script
        val reloadedScript = scriptManager.reloadScript(scriptName)
        assertNotNull(reloadedScript)

        // Execute the script after reloading (should use updated version)
        val resultAfterReload = scriptManager.execute(scriptName)
        assertEquals("Hello, Updated World!", resultAfterReload)
    }

    @Test
    fun testRollback() = runBlocking {
        // Define a simple script
        val scriptName = "test-script"
        val scriptSource = """
            val result = "Version 1"
            result
        """.trimIndent()

        // Compile and save the script (version 1)
        scriptManager.compileAndSave(scriptName, scriptSource)

        // Define an updated script
        val updatedScriptSource = """
            val result = "Version 2"
            result
        """.trimIndent()

        // Compile and save the updated script (version 2)
        scriptManager.compileAndSave(scriptName, updatedScriptSource)

        // Execute the script (should use version 2)
        val resultBeforeRollback = scriptManager.execute(scriptName)
        assertEquals("Version 2", resultBeforeRollback)

        // Roll back to version 1
        val rolledBackScript = scriptManager.rollback(scriptName, 1)
        assertNotNull(rolledBackScript)

        // Execute the script after rollback (should use version 1)
        val resultAfterRollback = scriptManager.execute(scriptName)
        assertEquals("Version 1", resultAfterRollback)
    }

    @Test
    fun testListScripts() = runBlocking {
        // Define some scripts
        val script1Name = "script1"
        val script1Source = """val result = "Script 1"; result"""
        val script2Name = "script2"
        val script2Source = """val result = "Script 2"; result"""

        // Compile and save the scripts
        scriptManager.compileAndSave(script1Name, script1Source)
        scriptManager.compileAndSave(script2Name, script2Source)

        // List the scripts
        val scripts = scriptManager.listScripts()

        // Verify the list
        assertEquals(2, scripts.size)
        assertTrue(scripts.contains(script1Name))
        assertTrue(scripts.contains(script2Name))
    }

    @Test
    fun testDeleteScript() = runBlocking {
        // Define a script
        val scriptName = "test-script"
        val scriptSource = """val result = "Hello, World!"; result"""

        // Compile and save the script
        scriptManager.compileAndSave(scriptName, scriptSource)

        // Verify that the script exists
        assertTrue(scriptManager.listScripts().contains(scriptName))

        // Delete the script
        val deleted = scriptManager.deleteScript(scriptName)
        assertTrue(deleted)

        // Verify that the script no longer exists
        assertFalse(scriptManager.listScripts().contains(scriptName))

        // Try to execute the deleted script
        val result = scriptManager.execute(scriptName)
        assertNull(result)
    }

    @Test
    fun testValidationFailure() = runBlocking {
        // Define an invalid script (unbalanced braces)
        val scriptName = "invalid-script"
        val scriptSource = """
            fun greet(name: String {
                println("Hello, ${'$'}name!")
            }
        """.trimIndent()

        // Try to compile and save the script
        val exception = assertFailsWith<ScriptValidationException> {
            scriptManager.compileAndSave(scriptName, scriptSource)
        }

        // Verify the exception message
        assertTrue(exception.message?.contains("validation failed") == true)
        assertTrue(exception.message?.contains("Unbalanced") == true)

        // Verify that the script was not saved
        assertFalse(scriptManager.listScripts().contains(scriptName))
    }
}
