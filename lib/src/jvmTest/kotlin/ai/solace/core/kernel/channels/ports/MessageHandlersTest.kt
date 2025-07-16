package ai.solace.core.kernel.channels.ports

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

class MessageHandlersTest {

    @Test
    fun testStringProtocolAdapterEncode() = runTest {
        // Create a StringProtocolAdapter
        val adapter = object : StringProtocolAdapter<Int>() {
            override suspend fun decode(target: String): Int {
                return target.toInt()
            }
        }
        
        // Test encoding an integer to a string
        val result = adapter.encode(42)
        
        // Verify the result
        assertEquals("42", result)
    }
    
    @Test
    fun testStringProtocolAdapterDecodeThrowsException() = runTest {
        // Create a base StringProtocolAdapter without overriding decode
        val adapter = object : StringProtocolAdapter<Int>() {}
        
        // Test that decode throws UnsupportedOperationException
        assertFailsWith<UnsupportedOperationException> {
            adapter.decode("42")
        }
    }
    
    @Test
    fun testStringProtocolAdapterCanHandle() = runTest {
        // Create a StringProtocolAdapter
        val adapter = object : StringProtocolAdapter<Int>() {
            override suspend fun decode(target: String): Int {
                return target.toInt()
            }
        }
        
        // Test canHandle with valid types
        assertTrue(adapter.canHandle(Int::class, String::class))
        
        // Test canHandle with invalid target type
        assertFalse(adapter.canHandle(Int::class, Int::class))
    }
    
    @Test
    fun testStringProtocolAdapterCompanionCreate() = runTest {
        // Create a StringProtocolAdapter using the companion create method
        val adapter = StringProtocolAdapter.create<Int> { it.toInt() }
        
        // Test encoding
        val encodedResult = adapter.encode(42)
        assertEquals("42", encodedResult)
        
        // Test decoding
        val decodedResult = adapter.decode("42")
        assertEquals(42, decodedResult)
        
        // Test canHandle
        assertTrue(adapter.canHandle(Int::class, String::class))
        assertFalse(adapter.canHandle(String::class, String::class))
        assertFalse(adapter.canHandle(Int::class, Int::class))
    }
    
    @Test
    fun testStringProtocolAdapterWithCustomType() = runTest {
        // Define a custom data class
        data class Person(val name: String, val age: Int)
        
        // Create a StringProtocolAdapter for the custom type
        val adapter = StringProtocolAdapter.create<Person> { str ->
            val parts = str.split(",")
            Person(parts[0], parts[1].toInt())
        }
        
        // Test encoding
        val person = Person("John", 30)
        val encodedResult = adapter.encode(person)
        assertEquals("Person(name=John, age=30)", encodedResult)
        
        // Test decoding
        val decodedResult = adapter.decode("John,30")
        assertEquals(Person("John", 30), decodedResult)
        
        // Test canHandle
        assertTrue(adapter.canHandle(Person::class, String::class))
        assertFalse(adapter.canHandle(String::class, String::class))
    }
}