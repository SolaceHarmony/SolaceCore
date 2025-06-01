package ai.solace.core.workflow

import ai.solace.core.actor.Actor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class WorkflowManagerTest {

    /**
     * A simple test actor for testing the workflow manager.
     */
    private class TestActor(
        id: String = "test-actor",
        name: String = "TestActor",
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    ) : Actor(id, name, scope) {
        var startCalled = false
        var stopCalled = false
        var disposeCalled = false

        // We can't override pause and resume as they're final in Actor
        // Instead, we'll track state changes to detect when they're called
        val stateChanges = mutableListOf<Pair<Any, Any>>()

        // Override onStateChange to track state transitions
        fun recordStateChange(oldState: Any, newState: Any) {
            stateChanges.add(Pair(oldState, newState))
        }

        override suspend fun start() {
            startCalled = true
            super.start()
        }

        override suspend fun stop() {
            stopCalled = true
            super.stop()
        }

        override suspend fun dispose() {
            disposeCalled = true
            super.dispose()
        }

        suspend fun createTestPort() {
            createPort(
                name = "test-port",
                messageClass = String::class,
                handler = { /* No-op */ },
                bufferSize = 10  // Use a positive buffer size
            )
        }
    }

    @Test
    fun testWorkflowLifecycle() = runBlocking {
        // Create a workflow manager
        val workflowManager = WorkflowManager(name = "TestWorkflow")

        // Initial state should be Initialized
        assertEquals(WorkflowState.Initialized, workflowManager.state)

        // Create test actors
        val actor1 = TestActor(id = "actor1", name = "Actor1")
        val actor2 = TestActor(id = "actor2", name = "Actor2")

        // Add actors to the workflow
        workflowManager.addActor(actor1)
        workflowManager.addActor(actor2)

        // Create test ports
        actor1.createTestPort()
        actor2.createTestPort()

        // Connect actors
        workflowManager.connectActors(actor1, "test-port", actor2, "test-port")

        // Start the workflow
        workflowManager.start()

        // Verify workflow state
        assertEquals(WorkflowState.Running, workflowManager.state)
        assertTrue(workflowManager.isActive())

        // Verify actors were started
        assertTrue(actor1.startCalled)
        assertTrue(actor2.startCalled)

        // Pause the workflow
        val pauseReason = "Test pause"
        workflowManager.pause(pauseReason)

        // Verify workflow state
        assertTrue(workflowManager.state is WorkflowState.Paused)
        assertEquals(pauseReason, (workflowManager.state as WorkflowState.Paused).reason)
        assertFalse(workflowManager.isActive())

        // In a real test, we would verify that actors were paused
        // Since we can't override pause/resume, we'll just verify the workflow state

        // Resume the workflow
        workflowManager.resume()

        // Verify workflow state
        assertEquals(WorkflowState.Running, workflowManager.state)
        assertTrue(workflowManager.isActive())

        // Stop the workflow
        workflowManager.stop()

        // Verify workflow state
        assertEquals(WorkflowState.Stopped, workflowManager.state)
        assertFalse(workflowManager.isActive())

        // Verify actors were stopped
        assertTrue(actor1.stopCalled)
        assertTrue(actor2.stopCalled)

        // Dispose the workflow
        workflowManager.dispose()

        // Verify actors were disposed
        assertTrue(actor1.disposeCalled)
        assertTrue(actor2.disposeCalled)
    }

    @Test
    fun testAddingActors() = runBlocking {
        // Create a workflow manager
        val workflowManager = WorkflowManager()

        // Create test actors
        val actor1 = TestActor(id = "actor1")
        val actor2 = TestActor(id = "actor2")
        val actor3 = TestActor(id = "actor3")

        // Add actors to the workflow
        workflowManager.addActor(actor1)
        workflowManager.addActor(actor2)
        workflowManager.addActor(actor3)

        // Verify actors were added
        val actors = workflowManager.getActors()
        assertEquals(3, actors.size)
        assertTrue(actors.contains(actor1))
        assertTrue(actors.contains(actor2))
        assertTrue(actors.contains(actor3))

        // Verify getting actor by ID
        assertEquals(actor1, workflowManager.getActor("actor1"))
        assertEquals(actor2, workflowManager.getActor("actor2"))
        assertEquals(actor3, workflowManager.getActor("actor3"))
        assertNull(workflowManager.getActor("non-existent"))
    }

    @Test
    fun testConnectingActors() = runBlocking {
        // Create a workflow manager
        val workflowManager = WorkflowManager()

        // Create test actors
        val actor1 = TestActor(id = "actor1")
        val actor2 = TestActor(id = "actor2")

        // Add actors to the workflow
        workflowManager.addActor(actor1)
        workflowManager.addActor(actor2)

        // Create test ports
        actor1.createTestPort()
        actor2.createTestPort()

        // Connect actors
        workflowManager.connectActors(actor1, "test-port", actor2, "test-port")

        // Verify connection was added
        val connections = workflowManager.getConnections()
        assertEquals(1, connections.size)

        val connection = connections.first()
        assertEquals("actor1", connection.sourceActorId)
        assertEquals("test-port", connection.sourcePortName)
        assertEquals("actor2", connection.targetActorId)
        assertEquals("test-port", connection.targetPortName)
    }

    @Test
    fun testErrorHandling(): Unit = runBlocking {
        // Create a workflow manager
        val workflowManager = WorkflowManager()

        // Try to add an actor with the same ID twice
        val actor = TestActor(id = "duplicate")
        workflowManager.addActor(actor)

        // This should throw an exception
        assertFailsWith<IllegalArgumentException> {
            runBlocking {
                workflowManager.addActor(actor)
            }
        }

        // Try to connect actors that aren't in the workflow
        val outsideActor = TestActor(id = "outside")

        // This should throw an exception
        assertFailsWith<IllegalArgumentException> {
            runBlocking {
                workflowManager.connectActors(outsideActor, "port", actor, "port")
            }
        }

        // Try to start the workflow, then add an actor
        workflowManager.start()

        // This should throw an exception
        assertFailsWith<IllegalStateException> {
            runBlocking {
                workflowManager.addActor(TestActor(id = "new-actor"))
            }
        }
    }
}
