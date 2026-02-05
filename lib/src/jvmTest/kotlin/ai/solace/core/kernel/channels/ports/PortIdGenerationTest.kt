package ai.solace.core.kernel.channels.ports

import kotlin.test.*

class PortIdGenerationTest {

    @Test
    fun testPortIdGeneration() {
        // Generate multiple IDs to ensure they are unique
        val ids = mutableSetOf<String>()
        repeat(1000) {
            val id = Port.generateId()
            assertFalse(ids.contains(id), "Duplicate ID generated: $id")
            ids.add(id)
        }
        
        // Verify all IDs have the expected format
        ids.forEach { id ->
            assertTrue(id.startsWith("port-"), "ID should start with 'port-': $id")
            assertTrue(id.length > 5, "ID should be longer than just the prefix: $id")
        }
    }

    @Test
    fun testPortIdFormat() {
        val id = Port.generateId()
        
        // Should start with "port-"
        assertTrue(id.startsWith("port-"))
        
        // Should be followed by a UUID
        val uuidPart = id.substring(5) // Remove "port-" prefix
        assertTrue(uuidPart.isNotEmpty())
        
        // UUID format validation (basic check)
        assertTrue(uuidPart.contains("-"), "UUID should contain hyphens")
        assertTrue(uuidPart.length == 36, "UUID should be 36 characters long")
    }

    @Test
    fun testPortIdUniqueness() {
        // Test that consecutive calls produce different IDs
        val id1 = Port.generateId()
        val id2 = Port.generateId()
        val id3 = Port.generateId()
        
        assertNotEquals(id1, id2)
        assertNotEquals(id2, id3)
        assertNotEquals(id1, id3)
    }
}