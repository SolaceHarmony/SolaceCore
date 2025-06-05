package ai.solace.core.lifecycle

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LifecycleTest {

    @Test
    fun testLifecycleStartStop() = runTest {
        // Create a test implementation of Lifecycle
        val testLifecycle = TestLifecycle()
        
        // Verify initial state
        assertFalse(testLifecycle.isActive())
        assertFalse(testLifecycle.isDisposed)
        
        // Start the lifecycle
        testLifecycle.start()
        
        // Verify the lifecycle is active
        assertTrue(testLifecycle.isActive())
        assertFalse(testLifecycle.isDisposed)
        
        // Stop the lifecycle
        testLifecycle.stop()
        
        // Verify the lifecycle is inactive
        assertFalse(testLifecycle.isActive())
        assertFalse(testLifecycle.isDisposed)
    }
    
    @Test
    fun testLifecycleDispose() = runTest {
        // Create a test implementation of Lifecycle
        val testLifecycle = TestLifecycle()
        
        // Start the lifecycle
        testLifecycle.start()
        
        // Verify the lifecycle is active
        assertTrue(testLifecycle.isActive())
        assertFalse(testLifecycle.isDisposed)
        
        // Dispose the lifecycle
        testLifecycle.dispose()
        
        // Verify the lifecycle is inactive and disposed
        assertFalse(testLifecycle.isActive())
        assertTrue(testLifecycle.isDisposed)
    }
    
    @Test
    fun testLifecycleStartAfterStop() = runTest {
        // Create a test implementation of Lifecycle
        val testLifecycle = TestLifecycle()
        
        // Start the lifecycle
        testLifecycle.start()
        
        // Verify the lifecycle is active
        assertTrue(testLifecycle.isActive())
        
        // Stop the lifecycle
        testLifecycle.stop()
        
        // Verify the lifecycle is inactive
        assertFalse(testLifecycle.isActive())
        
        // Start the lifecycle again
        testLifecycle.start()
        
        // Verify the lifecycle is active again
        assertTrue(testLifecycle.isActive())
    }
    
    @Test
    fun testLifecycleStartAfterDispose() = runTest {
        // Create a test implementation of Lifecycle
        val testLifecycle = TestLifecycle()
        
        // Start the lifecycle
        testLifecycle.start()
        
        // Verify the lifecycle is active
        assertTrue(testLifecycle.isActive())
        
        // Dispose the lifecycle
        testLifecycle.dispose()
        
        // Verify the lifecycle is inactive and disposed
        assertFalse(testLifecycle.isActive())
        assertTrue(testLifecycle.isDisposed)
        
        // Attempt to start the lifecycle after disposal
        testLifecycle.start()
        
        // Verify the lifecycle remains inactive (cannot be restarted after disposal)
        assertFalse(testLifecycle.isActive())
        assertTrue(testLifecycle.isDisposed)
    }
    
    // Test implementation of Lifecycle
    private class TestLifecycle : Lifecycle {
        private var active = false
        var isDisposed = false
            private set
        
        override suspend fun start() {
            if (!isDisposed) {
                active = true
            }
        }
        
        override suspend fun stop() {
            active = false
        }
        
        override fun isActive(): Boolean = active
        
        override suspend fun dispose() {
            active = false
            isDisposed = true
        }
    }
}