package ai.solace.core.lifecycle

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class DisposableTest {

    @Test
    fun testDispose() = runTest {
        // Create a test implementation of Disposable
        val testDisposable = TestDisposable()
        
        // Verify initial state
        assertFalse(testDisposable.isDisposed)
        
        // Dispose the object
        testDisposable.dispose()
        
        // Verify the object was disposed
        assertTrue(testDisposable.isDisposed)
    }
    
    @Test
    fun testCompanionDispose() = runTest {
        // Create multiple test implementations of Disposable
        val testDisposable1 = TestDisposable()
        val testDisposable2 = TestDisposable()
        val testDisposable3 = TestDisposable()
        
        // Verify initial state
        assertFalse(testDisposable1.isDisposed)
        assertFalse(testDisposable2.isDisposed)
        assertFalse(testDisposable3.isDisposed)
        
        // Dispose all objects using the companion method
        Disposable.dispose(testDisposable1, testDisposable2, testDisposable3)
        
        // Verify all objects were disposed
        assertTrue(testDisposable1.isDisposed)
        assertTrue(testDisposable2.isDisposed)
        assertTrue(testDisposable3.isDisposed)
    }
    
    @Test
    fun testSafeDisposeWithoutException() = runTest {
        // Create a test implementation of Disposable
        val testDisposable = TestDisposable()
        
        // Verify initial state
        assertFalse(testDisposable.isDisposed)
        
        // Safely dispose the object
        testDisposable.safeDispose()
        
        // Verify the object was disposed
        assertTrue(testDisposable.isDisposed)
    }
    
    @Test
    fun testSafeDisposeWithException() = runTest {
        // Create a test implementation of Disposable that throws an exception during disposal
        val testDisposable = TestDisposable(throwException = true)
        
        // Redirect standard output to capture the error message
        val originalOut = System.out
        val outContent = ByteArrayOutputStream()
        System.setOut(PrintStream(outContent))
        
        try {
            // Safely dispose the object
            testDisposable.safeDispose()
            
            // Verify the error message was printed
            val output = outContent.toString()
            assertTrue(output.contains("Error during disposal: Test exception"))
            
            // Verify the object was not disposed (since an exception was thrown)
            assertFalse(testDisposable.isDisposed)
        } finally {
            // Restore standard output
            System.setOut(originalOut)
        }
    }
    
    // Test implementation of Disposable
    private class TestDisposable(private val throwException: Boolean = false) : Disposable {
        var isDisposed = false
            private set
        
        override suspend fun dispose() {
            if (throwException) {
                throw Exception("Test exception")
            }
            isDisposed = true
        }
    }
}