@file:OptIn(ExperimentalCoroutinesApi::class)

package ai.solace.core.actor

import ai.solace.core.actor.Actor.Companion.DEFAULT_PROCESSING_TIMEOUT
import ai.solace.core.kernel.channels.ports.Port
import ai.solace.core.kernel.channels.ports.PortException
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.test.runTest
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.coroutines.*
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
    fun testSendMessageWhileActorNotRunningThrowsException() {
        runBlocking {
            val actor = TestActor()
            // Ensure the actor is not running

            // Use assertFailsWith to check for the IllegalStateException
            assertFailsWith<IllegalStateException> {
                val port = actor.createPort("testPort", String::class, handler = { }, processingTimeout = Duration.ZERO, bufferSize = 1)
                port.send("testMessage")
            }
        }
    }
}