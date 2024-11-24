package ai.solace.core.lifecycle

import kotlin.test.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LifecycleTest {
    private class TestLifecycle : Lifecycle {
        private val mutex = Mutex()
        private var isStarted = false
        private var isStopped = false
        private var isDisposed = false

        override suspend fun start() = mutex.withLock {
            require(!isDisposed) { "Cannot start disposed lifecycle" }
            isStarted = true
            isStopped = false
        }

        override suspend fun stop() = mutex.withLock {
            require(!isDisposed) { "Cannot stop disposed lifecycle" }
            isStopped = true
            isStarted = false
        }

        override fun isActive(): Boolean = isStarted && !isStopped && !isDisposed

        override suspend fun dispose() = mutex.withLock {
            stop()
            isDisposed = true
        }

        fun getState() = LifecycleState(isStarted, isStopped, isDisposed)
    }

    data class LifecycleState(
        val isStarted: Boolean,
        val isStopped: Boolean,
        val isDisposed: Boolean
    )

    @Test
    fun `test lifecycle start stop sequence`() = runTest {
        val lifecycle = TestLifecycle()

        assertFalse(lifecycle.isActive())

        lifecycle.start()
        assertTrue(lifecycle.isActive())
        assertEquals(LifecycleState(true, false, false), lifecycle.getState())

        lifecycle.stop()
        assertFalse(lifecycle.isActive())
        assertEquals(LifecycleState(false, true, false), lifecycle.getState())
    }

    @Test
    fun `test disposal prevents further operations`() = runTest {
        val lifecycle = TestLifecycle()

        lifecycle.start()
        assertTrue(lifecycle.isActive())

        lifecycle.dispose()
        assertFalse(lifecycle.isActive())
        assertEquals(LifecycleState(false, true, true), lifecycle.getState())

        assertFailsWith<IllegalStateException> {
            lifecycle.start()
        }
    }

    @Test
    fun `test multiple start stop cycles`() = runTest {
        val lifecycle = TestLifecycle()

        repeat(3) {
            lifecycle.start()
            assertTrue(lifecycle.isActive())

            lifecycle.stop()
            assertFalse(lifecycle.isActive())
        }
    }

    @Test
    fun `test immediate disposal`() = runTest {
        val lifecycle = TestLifecycle()

        lifecycle.dispose()
        assertFalse(lifecycle.isActive())
        assertEquals(LifecycleState(false, true, true), lifecycle.getState())
    }
}