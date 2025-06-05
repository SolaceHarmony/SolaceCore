package ai.solace.core.util

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import org.slf4j.Logger

class LoggerProviderTest {
    
    @Test
    fun testGetLoggerByClass() {
        // Get a logger for this class
        val logger = LoggerProvider.getLogger(LoggerProviderTest::class.java)
        
        // Verify the logger is not null
        assertNotNull(logger)
        
        // Verify the logger has the correct name (should be the fully qualified class name)
        assertEquals("ai.solace.core.util.LoggerProviderTest", logger.name)
    }
    
    @Test
    fun testGetLoggerByName() {
        // Get a logger with a specific name
        val loggerName = "test.logger"
        val logger = LoggerProvider.getLogger(loggerName)
        
        // Verify the logger is not null
        assertNotNull(logger)
        
        // Verify the logger has the correct name
        assertEquals(loggerName, logger.name)
    }
    
    @Test
    fun testLoggerExtensionProperty() {
        // Create a test object
        val testObject = TestObject()
        
        // Get the logger using the extension property
        val logger = testObject.logger
        
        // Verify the logger is not null
        assertNotNull(logger)
        
        // Verify the logger has the correct name (should be the fully qualified class name of TestObject)
        assertEquals("ai.solace.core.util.LoggerProviderTest\$TestObject", logger.name)
    }
    
    // Test class for the logger extension property test
    private class TestObject
}