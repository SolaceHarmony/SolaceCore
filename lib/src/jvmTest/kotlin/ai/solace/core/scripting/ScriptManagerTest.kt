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
        // Define a simple script
        val scriptName = "greeting-script"
        val scriptSource = """
            val greeting = "Hello, World!"
            greeting
        """.trimIndent()

        // Compile and save the script
        scriptManager.compileAndSave(scriptName, scriptSource)

        // Execute the script
        val result = scriptManager.execute(scriptName)

        // Verify the result
        assertEquals("Hello, World!", result)
    }

    @Test
    fun testReloadScript() = runBlocking {
        // Define a simple script
        val scriptName = "test-script"
        val scriptSource = """
            "Hello, World!"
        """.trimIndent()

        // Compile and save the script
        scriptManager.compileAndSave(scriptName, scriptSource)

        // Execute the script (should use the original version)
        val resultBeforeUpdate = scriptManager.execute(scriptName)
        assertEquals("Hello, World!", resultBeforeUpdate)

        // Define an updated script
        val updatedScriptSource = """
            "Hello, Updated World!"
        """.trimIndent()

        // Compile and save the updated script
        scriptManager.compileAndSave(scriptName, updatedScriptSource)

        // Execute the script after update (should use the updated version)
        val resultAfterUpdate = scriptManager.execute(scriptName)
        assertEquals("Hello, Updated World!", resultAfterUpdate)

        // Now let's test the reloadScript method
        // First, save a different script directly to storage (bypassing the cache)
        val revertedScriptSource = """
            "Hello, World!"
        """.trimIndent()
        scriptStorage.saveScript(scriptName, revertedScriptSource)

        // Execute the script without reloading (should still use cached version)
        val resultBeforeReload = scriptManager.execute(scriptName)
        assertEquals("Hello, Updated World!", resultBeforeReload)

        // Reload the script
        val reloadedScript = scriptManager.reloadScript(scriptName)
        assertNotNull(reloadedScript)

        // Execute the script after reloading (should use the version from storage)
        val resultAfterReload = scriptManager.execute(scriptName)
        assertEquals("Hello, World!", resultAfterReload)
    }

    @Test
    fun testRollback() = runBlocking {
        // Define a simple script
        val scriptName = "test-script-rollback"
        val scriptSource = """
            "Version 1"
        """.trimIndent()

        println("[DEBUG_LOG] Compiling and saving version 1")
        // Compile and save the script (version 1)
        val compiledScript1 = scriptManager.compileAndSave(scriptName, scriptSource)
        println("[DEBUG_LOG] Compiled script 1: $compiledScript1")

        // Execute the script (should use version 1)
        val resultVersion1 = scriptManager.execute(scriptName)
        println("[DEBUG_LOG] Result version 1: '$resultVersion1'")
        assertEquals("Version 1", resultVersion1)

        // Define an updated script
        val updatedScriptSource = """
            "Version 2"
        """.trimIndent()

        println("[DEBUG_LOG] Compiling and saving version 2")
        // Compile and save the updated script (version 2)
        val compiledScript2 = scriptManager.compileAndSave(scriptName, updatedScriptSource)
        println("[DEBUG_LOG] Compiled script 2: $compiledScript2")

        // Execute the script (should use version 2)
        val resultVersion2 = scriptManager.execute(scriptName)
        println("[DEBUG_LOG] Result version 2: '$resultVersion2'")
        assertEquals("Version 2", resultVersion2)

        println("[DEBUG_LOG] Rolling back to version 1")
        // Roll back to version 1
        val rolledBackScript = scriptManager.rollback(scriptName, 1)
        println("[DEBUG_LOG] Rolled back script: $rolledBackScript")
        assertNotNull(rolledBackScript)

        // Execute the script after rollback (should use version 1)
        val resultAfterRollback = scriptManager.execute(scriptName)
        println("[DEBUG_LOG] Result after rollback: '$resultAfterRollback'")
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
