package ai.solace.core.actor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ActorStateTest {

    @Test
    fun testInitializedState() {
        // Create an Initialized state
        val state = ActorState.Initialized

        // Verify the toString method
        assertEquals("Initialized", state.toString())
    }

    @Test
    fun testRunningState() {
        // Create a Running state
        val state = ActorState.Running

        // Verify the toString method
        assertEquals("Running", state.toString())
    }

    @Test
    fun testStoppedState() {
        // Create a Stopped state
        val state = ActorState.Stopped

        // Verify the toString method
        assertEquals("Stopped", state.toString())
    }

    @Test
    fun testErrorState() {
        // Create an Error state
        val errorMessage = "Test error message"
        val state = ActorState.Error(errorMessage)

        // Verify the exception property
        assertEquals(errorMessage, state.exception)

        // Verify the toString method
        assertEquals("Error(exception=$errorMessage)", state.toString())

        // Test equality
        val sameState = ActorState.Error(errorMessage)
        val differentState = ActorState.Error("Different error message")

        assertEquals(state, sameState)
        assertNotEquals(state, differentState)
    }

    @Test
    fun testPausedState() {
        // Create a Paused state
        val reason = "Test pause reason"
        val state = ActorState.Paused(reason)

        // Verify the reason property
        assertEquals(reason, state.reason)

        // Verify the toString method
        assertEquals("Paused(reason=$reason)", state.toString())

        // Test equality
        val sameState = ActorState.Paused(reason)
        val differentState = ActorState.Paused("Different pause reason")

        assertEquals(state, sameState)
        assertNotEquals(state, differentState)
    }

    @Test
    fun testStateTransitions() {
        // This test verifies that we can create different states and they are distinct

        // Create different states
        val initializedState = ActorState.Initialized
        val runningState = ActorState.Running
        val stoppedState = ActorState.Stopped
        val errorState = ActorState.Error("Test error")
        val pausedState = ActorState.Paused("Test pause")

        // Verify they are all different
        assertNotEquals<ActorState>(initializedState, runningState)
        assertNotEquals<ActorState>(initializedState, stoppedState)
        assertNotEquals<ActorState>(initializedState, errorState)
        assertNotEquals<ActorState>(initializedState, pausedState)

        assertNotEquals<ActorState>(runningState, stoppedState)
        assertNotEquals<ActorState>(runningState, errorState)
        assertNotEquals<ActorState>(runningState, pausedState)

        assertNotEquals<ActorState>(stoppedState, errorState)
        assertNotEquals<ActorState>(stoppedState, pausedState)

        assertNotEquals<ActorState>(errorState, pausedState)
    }

    @Test
    fun testSealedClassExhaustiveness() {
        // This test verifies that we can handle all possible states in a when expression

        fun getStateDescription(state: ActorState): String {
            return when (state) {
                is ActorState.Initialized -> "Actor is initialized"
                is ActorState.Running -> "Actor is running"
                is ActorState.Stopped -> "Actor is stopped"
                is ActorState.Error -> "Actor has an error: ${state.exception}"
                is ActorState.Paused -> "Actor is paused: ${state.reason}"
            }
        }

        // Test the function with each state
        assertEquals("Actor is initialized", getStateDescription(ActorState.Initialized))
        assertEquals("Actor is running", getStateDescription(ActorState.Running))
        assertEquals("Actor is stopped", getStateDescription(ActorState.Stopped))
        assertEquals("Actor has an error: Test error", getStateDescription(ActorState.Error("Test error")))
        assertEquals("Actor is paused: Test pause", getStateDescription(ActorState.Paused("Test pause")))
    }
}
