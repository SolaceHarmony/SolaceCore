package ai.solace.core.actor.supervisor

import ai.solace.core.actor.Actor
import ai.solace.core.actor.ActorState
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class SupervisorActorTest {

    // Test actor class for testing the SupervisorActor
    private class TestActor : Actor() {
        var customState: String = "initial"
    }

    @Test
    fun testRegisterActor() = runTest {
        val supervisor = SupervisorActor()
        val actor = TestActor()

        // Start the supervisor
        supervisor.start()
        assertEquals(ActorState.Running, supervisor.state)

        // Register the actor
        val result = supervisor.registerActor(actor)
        assertTrue(result)

        // Verify the actor is registered
        val retrievedActor = supervisor.getActor(actor.id)
        assertNotNull(retrievedActor)
        assertEquals(actor.id, retrievedActor.id)
    }

    @Test
    fun testRegisterDuplicateActor() = runTest {
        val supervisor = SupervisorActor()
        val actor = TestActor()

        // Start the supervisor
        supervisor.start()

        // Register the actor
        val result1 = supervisor.registerActor(actor)
        assertTrue(result1)

        // Try to register the same actor again
        val result2 = supervisor.registerActor(actor)
        assertFalse(result2)
    }

    @Test
    fun testRegisterActorWhileSupervisorNotRunning() = runTest {
        val supervisor = SupervisorActor()
        val actor = TestActor()

        // Try to register the actor while the supervisor is not running
        assertFailsWith<IllegalStateException> {
            supervisor.registerActor(actor)
        }
    }

    @Test
    fun testUnregisterActor() = runTest {
        val supervisor = SupervisorActor()
        val actor = TestActor()

        // Start the supervisor
        supervisor.start()

        // Register the actor
        supervisor.registerActor(actor)

        // Unregister the actor
        val result = supervisor.unregisterActor(actor.id)
        assertTrue(result)

        // Verify the actor is unregistered
        val retrievedActor = supervisor.getActor(actor.id)
        assertNull(retrievedActor)
    }

    @Test
    fun testUnregisterNonExistentActor() = runTest {
        val supervisor = SupervisorActor()

        // Start the supervisor
        supervisor.start()

        // Try to unregister a non-existent actor
        val result = supervisor.unregisterActor("non-existent-id")
        assertFalse(result)
    }

    @Test
    fun testUnregisterActorWhileSupervisorNotRunning() = runTest {
        val supervisor = SupervisorActor()

        // Try to unregister an actor while the supervisor is not running
        assertFailsWith<IllegalStateException> {
            supervisor.unregisterActor("some-id")
        }
    }

    @Test
    fun testHotSwapActor() = runTest {
        val supervisor = SupervisorActor()
        val oldActor = TestActor()
        oldActor.customState = "old"

        // Start the supervisor
        supervisor.start()

        // Register the old actor
        supervisor.registerActor(oldActor)

        // Start the old actor
        oldActor.start()

        // Print the old actor's state before hot-swapping
        println("[DEBUG_LOG] Old actor state before hot-swap: ${oldActor.state}")

        // Create a new actor
        val newActor = TestActor()
        newActor.customState = "new"

        // Hot-swap the actor
        val result = supervisor.hotSwapActor(oldActor.id, newActor)
        assertTrue(result)

        // Get the new actor
        val retrievedActor = supervisor.getActor(oldActor.id) as? TestActor

        // Verify the new actor is registered
        assertNotNull(retrievedActor)
        assertEquals("new", retrievedActor.customState)

        // Verify the new actor is running
        assertEquals(ActorState.Running, retrievedActor.state)
    }

    @Test
    fun testHotSwapNonExistentActor() = runTest {
        val supervisor = SupervisorActor()
        val newActor = TestActor()

        // Start the supervisor
        supervisor.start()

        // Try to hot-swap a non-existent actor
        val result = supervisor.hotSwapActor("non-existent-id", newActor)
        assertFalse(result)
    }

    @Test
    fun testHotSwapActorWithDifferentType() = runTest {
        val supervisor = SupervisorActor()
        val oldActor = TestActor()

        // Start the supervisor
        supervisor.start()

        // Register the old actor
        supervisor.registerActor(oldActor)

        // Create a new actor of a different type
        val newActor = object : Actor() {
            // Different type
        }

        // Try to hot-swap with an actor of a different type
        val result = supervisor.hotSwapActor(oldActor.id, newActor)
        assertFalse(result)
    }

    @Test
    fun testHotSwapActorWhileSupervisorNotRunning() = runTest {
        val supervisor = SupervisorActor()
        val oldActor = TestActor()
        val newActor = TestActor()

        // Try to hot-swap an actor while the supervisor is not running
        assertFailsWith<IllegalStateException> {
            supervisor.hotSwapActor(oldActor.id, newActor)
        }
    }

    @Test
    fun testGetActorsByType() = runTest {
        val supervisor = SupervisorActor()
        val actor1 = TestActor()
        val actor2 = TestActor()
        val actor3 = object : Actor() {
            // Different type
        }

        // Start the supervisor
        supervisor.start()

        // Register the actors
        supervisor.registerActor(actor1)
        supervisor.registerActor(actor2)
        supervisor.registerActor(actor3)

        // Get actors by type
        val testActors = supervisor.getActorsByType(TestActor::class)
        assertEquals(2, testActors.size)
        assertTrue(testActors.contains(actor1))
        assertTrue(testActors.contains(actor2))
        assertFalse(testActors.contains(actor3))
    }

    @Test
    fun testStartAllActors() = runTest {
        val supervisor = SupervisorActor()
        val actor1 = TestActor()
        val actor2 = TestActor()

        // Start the supervisor
        supervisor.start()

        // Register the actors
        supervisor.registerActor(actor1)
        supervisor.registerActor(actor2)

        // Start all actors
        supervisor.startAllActors()

        // Verify all actors are running
        assertEquals(ActorState.Running, actor1.state)
        assertEquals(ActorState.Running, actor2.state)
    }

    @Test
    fun testStartAllActorsWhileSupervisorNotRunning() = runTest {
        val supervisor = SupervisorActor()

        // Try to start all actors while the supervisor is not running
        assertFailsWith<IllegalStateException> {
            supervisor.startAllActors()
        }
    }

    @Test
    fun testStopAllActors() = runTest {
        val supervisor = SupervisorActor()
        val actor1 = TestActor()
        val actor2 = TestActor()

        // Start the supervisor
        supervisor.start()

        // Register the actors
        supervisor.registerActor(actor1)
        supervisor.registerActor(actor2)

        // Start the actors
        actor1.start()
        actor2.start()

        // Stop all actors
        supervisor.stopAllActors()

        // Verify all actors are stopped
        assertEquals(ActorState.Stopped, actor1.state)
        assertEquals(ActorState.Stopped, actor2.state)
    }

    @Test
    fun testStopAllActorsWhileSupervisorNotRunning() = runTest {
        val supervisor = SupervisorActor()

        // Try to stop all actors while the supervisor is not running
        assertFailsWith<IllegalStateException> {
            supervisor.stopAllActors()
        }
    }

    @Test
    fun testDispose() = runTest {
        val supervisor = SupervisorActor()
        val actor1 = TestActor()
        val actor2 = TestActor()

        // Start the supervisor
        supervisor.start()

        // Register the actors
        supervisor.registerActor(actor1)
        supervisor.registerActor(actor2)

        // Start the actors
        actor1.start()
        actor2.start()

        // Dispose the supervisor
        supervisor.dispose()

        // Verify all actors are disposed
        assertEquals(ActorState.Stopped, actor1.state)
        assertEquals(ActorState.Stopped, actor2.state)

        // Verify the registry is cleared
        assertEquals(0, supervisor.getAllActors().size)
    }
}
