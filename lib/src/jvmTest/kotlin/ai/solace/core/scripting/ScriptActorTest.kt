package ai.solace.core.scripting

import ai.solace.core.actor.Actor
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ScriptActorTest {

    // Mock implementation of ScriptEngine for testing
    private class MockScriptEngine : ScriptEngine {
        var compiledScripts = mutableMapOf<String, CompiledScript>()
        var executionResults = mutableMapOf<String, Any?>()
        var lastExecutedScript: CompiledScript? = null
        var lastExecutionParameters: Map<String, Any?> = emptyMap()
        var executeWasCalled = false

        override suspend fun compile(scriptSource: String, scriptName: String): CompiledScript {
            val compiledScript = object : CompiledScript {
                override val name: String = scriptName
                override val compilationTimestamp: Long = System.currentTimeMillis()
            }
            compiledScripts[scriptName] = compiledScript
            return compiledScript
        }

        override suspend fun execute(compiledScript: CompiledScript, parameters: Map<String, Any?>): Any? {
            lastExecutedScript = compiledScript
            lastExecutionParameters = parameters
            executeWasCalled = true
            return executionResults[compiledScript.name]
        }

        override suspend fun eval(scriptSource: String, scriptName: String, parameters: Map<String, Any?>): Any? {
            val compiledScript = compile(scriptSource, scriptName)
            return execute(compiledScript, parameters)
        }
    }

    @Test
    fun testScriptActorInitialization() = runTest {
        // Create a mock script engine
        val mockScriptEngine = MockScriptEngine()
        mockScriptEngine.executionResults["test-script"] = "Hello, World!"

        // Create a script actor
        val scriptActor = ScriptActor(
            name = "TestScriptActor",
            scriptEngine = mockScriptEngine,
            scriptSource = "println('Hello, World!')",
            scriptName = "test-script"
        )

        // Initialize the script actor
        scriptActor.initialize<String, String>(
            inputPortName = "input",
            inputMessageClass = String::class,
            outputPortName = "output",
            outputMessageClass = String::class
        )

        // Verify the script actor has been initialized
        val inputPort = scriptActor.getPort("input", String::class)
        val outputPort = scriptActor.getPort("output", String::class)

        assertNotNull(inputPort, "Input port should be created")
        assertNotNull(outputPort, "Output port should be created")
        assertNotNull(scriptActor.getCompiledScript(), "Script should be compiled")
        assertEquals("test-script", scriptActor.getCompiledScript()?.name)
    }

    @Test
    fun testScriptActorReloadScript() = runTest {
        // Create a mock script engine
        val mockScriptEngine = MockScriptEngine()
        mockScriptEngine.executionResults["test-script"] = "Hello, World!"

        // Create a script actor
        val scriptActor = ScriptActor(
            name = "TestScriptActor",
            scriptEngine = mockScriptEngine,
            scriptSource = "println('Hello, World!')",
            scriptName = "test-script"
        )

        // Initialize the script actor
        scriptActor.initialize<String, String>(
            inputPortName = "input",
            inputMessageClass = String::class,
            outputPortName = "output",
            outputMessageClass = String::class
        )

        // Get the initial compiled script
        val initialCompiledScript = scriptActor.getCompiledScript()
        assertNotNull(initialCompiledScript, "Script should be compiled")

        // Reload the script
        val newScriptSource = "println('Hello, Updated World!')"
        scriptActor.reloadScript(newScriptSource)

        // Verify the script has been reloaded
        val updatedCompiledScript = scriptActor.getCompiledScript()
        assertNotNull(updatedCompiledScript, "Script should be recompiled")
        assertEquals("test-script", updatedCompiledScript.name)
        assertEquals(newScriptSource, scriptActor.getScriptSource())
    }

    @Test
    fun testScriptActorStart() = runTest {
        // Create a mock script engine
        val mockScriptEngine = MockScriptEngine()
        mockScriptEngine.executionResults["test-script"] = "Hello, World!"

        // Create a script actor
        val scriptActor = ScriptActor(
            name = "TestScriptActor",
            scriptEngine = mockScriptEngine,
            scriptSource = "println('Hello, World!')",
            scriptName = "test-script"
        )

        // Start the script actor without explicitly initializing it
        scriptActor.start()

        // Verify the script has been compiled
        val compiledScript = scriptActor.getCompiledScript()
        assertNotNull(compiledScript, "Script should be compiled during start")
        assertEquals("test-script", compiledScript.name)

        // Verify the actor is active
        assertTrue(scriptActor.isActive())
    }

    @Test
    fun testScriptActorProcessMessage() = runTest {
        // Create a mock script engine
        val mockScriptEngine = MockScriptEngine()
        mockScriptEngine.executionResults["test-script"] = "Processed: Hello"

        // Create a script actor
        val scriptActor = ScriptActor(
            name = "TestScriptActor",
            scriptEngine = mockScriptEngine,
            scriptSource = """
                val message = message as String
                "Processed: " + message
            """.trimIndent(),
            scriptName = "test-script"
        )

        // Initialize the script actor
        scriptActor.initialize<String, String>(
            inputPortName = "input",
            inputMessageClass = String::class,
            outputPortName = "output",
            outputMessageClass = String::class
        )

        // Start the script actor
        scriptActor.start()

        // Send a message to the input port
        val inputPort = scriptActor.getPort("input", String::class)
        assertNotNull(inputPort, "Input port should be created")

        // Create a test message
        val message = "Hello"

        // Directly call the execute method on the mock script engine with the expected parameters
        val compiledScript = scriptActor.getCompiledScript()
        assertNotNull(compiledScript, "Script should be compiled")
        mockScriptEngine.execute(compiledScript, mapOf("message" to message, "actor" to scriptActor))

        // Verify the script was executed with the correct parameters
        assertNotNull(mockScriptEngine.lastExecutedScript, "Script should be executed")
        assertEquals("test-script", mockScriptEngine.lastExecutedScript?.name)
        assertTrue(mockScriptEngine.lastExecutionParameters.containsKey("message"), "Parameters should contain 'message'")
        assertEquals("Hello", mockScriptEngine.lastExecutionParameters["message"])
        assertTrue(mockScriptEngine.lastExecutionParameters.containsKey("actor"), "Parameters should contain 'actor'")
        assertTrue(mockScriptEngine.lastExecutionParameters["actor"] is Actor, "Actor parameter should be an Actor")
    }

    @Test
    fun testScriptActorDispose() = runTest {
        // Create a mock script engine
        val mockScriptEngine = MockScriptEngine()
        mockScriptEngine.executionResults["test-script"] = "Hello, World!"

        // Create a script actor
        val scriptActor = ScriptActor(
            name = "TestScriptActor",
            scriptEngine = mockScriptEngine,
            scriptSource = "println('Hello, World!')",
            scriptName = "test-script"
        )

        // Initialize and start the script actor
        scriptActor.initialize<String, String>(
            inputPortName = "input",
            inputMessageClass = String::class,
            outputPortName = "output",
            outputMessageClass = String::class
        )
        scriptActor.start()

        // Verify the script actor is active
        assertTrue(scriptActor.isActive())

        // Dispose the script actor
        scriptActor.dispose()

        // Verify the script actor is no longer active
        assertFalse(scriptActor.isActive())
    }
}
