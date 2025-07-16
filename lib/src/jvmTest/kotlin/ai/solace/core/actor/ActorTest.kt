@file:OptIn(ExperimentalCoroutinesApi::class)

package ai.solace.core.actor

import ai.solace.core.kernel.channels.ports.Port
import ai.solace.core.kernel.channels.ports.PortException
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("unused")
@ExperimentalCoroutinesApi
@ExperimentalStdlibApi
@ExperimentalUnsignedTypes

class ActorTest {

    private open class TestActor : Actor() {
        suspend fun exposeCreatePort(
            name: String,
            messageClass: KClass<String>,
            handler: suspend (String) -> Unit,
            bufferSize: Int = Channel.BUFFERED,
            processingTimeout: Duration = DEFAULT_PROCESSING_TIMEOUT
        ): Port<String> {
            require(bufferSize > 0)
            return createPort(name, messageClass, handler, bufferSize, processingTimeout)
        }
    }

    @Test
    fun testCreatePort() = runTest {
        val actor = TestActor()
        val portName = "testPort"
        val messageClass = String::class
        val handler: suspend (String) -> Unit = { _ -> }

        val port = actor.exposeCreatePort(portName, messageClass, handler,bufferSize = 10)
        assertNotNull(port)
        assertEquals(portName, port.name)
    }

    @Test(expected = PortException.Validation::class)
    fun testCreatePortWithDuplicateName() = runTest {
        val actor = TestActor()
        val portName = "testPort"
        val messageClass = String::class
        val handler: suspend (String) -> Unit = { _ -> }

        actor.exposeCreatePort(portName, messageClass, handler, bufferSize = 1)
        actor.exposeCreatePort(portName, messageClass, handler, bufferSize = 1)  // Should throw exception
    }

    @Test
    fun testGetPort() = runTest {
        val actor = TestActor()
        val portName = "testPort"
        val messageClass = String::class
        val handler: suspend (String) -> Unit = { _ -> }

        actor.exposeCreatePort(portName, messageClass, handler, bufferSize = 1)
        val port = actor.getPort(portName, messageClass)
        assertNotNull(port)
        assertEquals(portName, port?.name)
    }

    @Test
    fun testStateTransition() = runTest {
        val actor = TestActor()

        assertEquals(ActorState.Initialized, actor.state)
        runBlocking { actor.start() }
        assertEquals(ActorState.Running, actor.state)
        runBlocking { actor.stop() }
        assertEquals(ActorState.Stopped, actor.state)
        runBlocking { actor.dispose() }
        assertEquals(ActorState.Stopped, actor.state)
    }

    @Test
    fun testSendMessageWhileActorNotRunningThrowsException() = runTest {
        val actor = TestActor()

        // Create a port while the actor is not running
        val port = actor.createPort("testPort", String::class, handler = { }, processingTimeout = Duration.ZERO, bufferSize = 1)

        // Explicitly check that the actor is not running
        assert(!actor.isActive())

        // Verify the actor's state is Initialized
        assertEquals(ActorState.Initialized, actor.state)

        // Start the actor
        actor.start()

        // Verify the actor's state is Running
        assertEquals(ActorState.Running, actor.state)

        // Now we can send a message without an exception
        port.send("testMessage")

        // Clean up
        actor.stop()
    }

    @Test
    fun testRemovePort() = runTest {
        val actor = TestActor()
        val portName = "testPort"
        val messageClass = String::class
        val handler: suspend (String) -> Unit = { _ -> }

        // Create a port
        val port = actor.exposeCreatePort(portName, messageClass, handler, bufferSize = 1)
        assertNotNull(port)

        // Start the actor
        actor.start()

        // Remove the port
        val result = actor.removePort(portName)
        assertEquals(true, result)

        // Verify the port is removed
        val retrievedPort = actor.getPort(portName, messageClass)
        assertEquals(null, retrievedPort)
    }

    @Test
    fun testRemoveNonExistentPort() = runTest {
        val actor = TestActor()
        actor.start()

        // Try to remove a port that doesn't exist
        val result = actor.removePort("nonExistentPort")
        assertEquals(false, result)
    }

    @Test
    fun testRemovePortWhileActorNotRunning() = runTest {
        val actor = TestActor()
        val portName = "testPort"
        val messageClass = String::class
        val handler: suspend (String) -> Unit = { _ -> }

        // Create a port
        actor.exposeCreatePort(portName, messageClass, handler, bufferSize = 1)

        // Stop the actor to ensure it's not in the Running state
        actor.stop()

        // Try to remove the port while the actor is stopped
        assertFailsWith<IllegalStateException> {
            actor.removePort(portName)
        }
    }

    @Test
    fun testRecreatePort() = runTest {
        val actor = TestActor()
        val portName = "testPort"
        val messageClass = String::class
        val handler1: suspend (String) -> Unit = { _ -> }
        val handler2: suspend (String) -> Unit = { _ -> }

        // Start the actor first
        actor.start()

        // Create a port
        val port1 = actor.exposeCreatePort(portName, messageClass, handler1, bufferSize = 1)
        assertNotNull(port1)

        // Recreate the port with a new handler
        val port2 = actor.recreatePort(portName, messageClass, handler2, bufferSize = 1)
        assertNotNull(port2)

        // Verify the port is recreated
        val retrievedPort = actor.getPort(portName, messageClass)
        assertNotNull(retrievedPort)

        // Clean up
        actor.stop()
    }

    @Test
    fun testRecreateNonExistentPort() = runTest {
        val actor = TestActor()
        actor.start()

        // Try to recreate a port that doesn't exist
        val result = actor.recreatePort("nonExistentPort", String::class, { _ -> })
        assertEquals(null, result)
    }

    @Test
    fun testDisconnectPort() = runTest {
        val actor = TestActor()
        val portName = "testPort"
        val messageClass = String::class
        val handler: suspend (String) -> Unit = { _ -> }

        // Create a port
        val port1 = actor.exposeCreatePort(portName, messageClass, handler, bufferSize = 1)
        assertNotNull(port1)

        // Start the actor
        actor.start()

        // Disconnect the port
        val port2 = actor.disconnectPort(portName, messageClass)
        assertNotNull(port2)

        // Verify the port is still there but disconnected
        val retrievedPort = actor.getPort(portName, messageClass)
        assertNotNull(retrievedPort)
    }

    @Test
    fun testDisconnectNonExistentPort() = runTest {
        val actor = TestActor()
        actor.start()

        // Try to disconnect a port that doesn't exist
        val result = actor.disconnectPort("nonExistentPort", String::class)
        assertEquals(null, result)
    }
}
