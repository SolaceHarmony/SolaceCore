package ai.solace.core.actor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ActorTest {
    
    private class TestActor : Actor(scope = kotlinx.coroutines.CoroutineScope(UnconfinedTestDispatcher())) {
        var messageProcessed = false
        var lastMessage: ActorMessage? = null
        
        override fun defineInterface() {
            interface.input("test", String::class)
            interface.output("result", String::class)
        }
        
        override suspend fun processMessage(message: ActorMessage) {
            messageProcessed = true
            lastMessage = message
        }
    }
    
    @Test
    fun `test actor creation and interface definition`() {
        val actor = TestActor()
        
        assertNotNull(actor.getInterface().getInput("test"))
        assertNotNull(actor.getInterface().getOutput("result"))
    }
    
    @Test
    fun `test actor message processing`() = runTest {
        val actor = TestActor()
        val message = Actor.ActorMessage(
            type = "test",
            payload = "test data"
        )
        
        actor.start()
        actor.send(message)
        
        assertTrue(actor.messageProcessed)
        assertEquals(message, actor.lastMessage)
    }
    
    @Test
    fun `test actor lifecycle`() = runTest {
        val actor = TestActor()
        
        // Start actor
        actor.start()
        
        // Send message
        val message = Actor.ActorMessage(
            type = "test",
            payload = "test data"
        )
        actor.send(message)
        
        // Verify message was processed
        assertTrue(actor.messageProcessed)
        
        // Stop actor
        actor.stop()
        
        // Reset flag and try sending another message
        actor.messageProcessed = false
        try {
            actor.send(message)
        } catch (_: Exception) {
            // Channel might be closed, which is expected
        }
        
        // Verify no new messages were processed
        assertFalse(actor.messageProcessed)
    }
    
    @Test
    fun `test actor error handling`() = runTest {
        class ErrorActor : Actor(scope = kotlinx.coroutines.CoroutineScope(UnconfinedTestDispatcher())) {
            var errorHandled = false
            
            override fun defineInterface() {}
            
            override suspend fun processMessage(message: ActorMessage) {
                throw RuntimeException("Test error")
            }
            
            override fun handleError(error: Exception, message: ActorMessage) {
                errorHandled = true
            }
        }
        
        val actor = ErrorActor()
        actor.start()
        
        actor.send(Actor.ActorMessage(type = "test", payload = "data"))
        
        assertTrue(actor.errorHandled)
    }
}