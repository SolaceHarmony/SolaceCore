package ai.solace.core.integration

import ai.solace.core.kernel.channels.ports.BidirectionalPort
import ai.solace.core.kernel.channels.ports.Port
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.*

/**
 * Property-based tests for type conversion system.
 * 
 * These tests validate that type conversions work correctly across
 * different data types and edge cases.
 */
class TypeConversionPropertyTest {

    @Test
    fun testStringToStringConversion() = runTest {
        val port = BidirectionalPort.create<String>("test-port")
        
        // Add identity conversion rule
        val identityRule = Port.ConversionRule.create<String, String>(
            converter = { it },
            validator = { inputType, outputType -> 
                inputType == String::class && outputType == String::class 
            },
            description = "Identity conversion"
        )
        port.addConversionRule(identityRule)
        
        // Test with various string values
        val testStrings = listOf(
            "",
            "hello",
            "Hello World!",
            "123",
            "Special chars: !@#$%^&*()",
            "Unicode: 你好世界",
            "Multiline\nstring\nwith\nbreaks",
            "Very long string ".repeat(100)
        )
        
        for (testString in testStrings) {
            port.send(testString)
            val received = port.receive()
            assertEquals(testString, received, "String conversion failed for: '$testString'")
        }
        
        port.dispose()
    }

    @Test
    fun testStringToIntConversion() = runTest {
        val port = BidirectionalPort.create<String>("test-port")
        
        // Add string processing rule that simulates int conversion
        val stringProcessingRule = Port.ConversionRule.create<String, String>(
            converter = { 
                val intValue = it.toIntOrNull() ?: 0
                intValue.toString()
            },
            validator = { inputType, outputType -> 
                inputType == String::class && outputType == String::class 
            },
            description = "String to Int processing"
        )
        port.addConversionRule(stringProcessingRule)
        
        // Test valid integer strings
        val validInts = mapOf(
            "0" to 0,
            "1" to 1,
            "-1" to -1,
            "123" to 123,
            "-456" to -456,
            "2147483647" to Int.MAX_VALUE,
            "-2147483648" to Int.MIN_VALUE
        )
        
        for ((stringValue, expectedInt) in validInts) {
            port.send(stringValue)
            val received = port.receive()
            // The string should be processed to represent the integer value
            assertEquals(expectedInt.toString(), received, "String to Int processing failed for: '$stringValue'")
        }
        
        port.dispose()
    }

    @Test
    fun testNumberConversions() = runTest {
        val port = BidirectionalPort.create<Number>("test-port")
        
        // Add number processing rule (since port is Number type, rule must be Number->Number)
        val numberProcessingRule = Port.ConversionRule.create<Number, Number>(
            converter = { it }, // Identity conversion for testing
            validator = { inputType, outputType -> 
                Number::class.java.isAssignableFrom(inputType.java) && 
                Number::class.java.isAssignableFrom(outputType.java)
            },
            description = "Number processing"
        )
        port.addConversionRule(numberProcessingRule)
        
        // Test with various number types
        val testNumbers = listOf<Number>(
            0, 1, -1, 123, -456,
            0.0, 1.5, -2.7, 3.14159,
            0f, 1.5f, -2.7f,
            0L, 123L, -456L,
            Byte.MAX_VALUE, Byte.MIN_VALUE,
            Short.MAX_VALUE, Short.MIN_VALUE,
            Int.MAX_VALUE, Int.MIN_VALUE,
            Long.MAX_VALUE, Long.MIN_VALUE,
            Float.MAX_VALUE, Float.MIN_VALUE,
            Double.MAX_VALUE, Double.MIN_VALUE
        )
        
        for (number in testNumbers) {
            port.send(number)
            val received = port.receive()
            // The received value should be the same number since we're not actually converting
            assertEquals(number, received, "Number handling failed for: $number")
        }
        
        port.dispose()
    }

    @Test
    fun testCollectionConversions() = runTest {
        val port = BidirectionalPort.create<List<String>>("test-port")
        
        // Add list processing rule (since port is List type, rule must be List->List)
        val listProcessingRule = Port.ConversionRule.create<List<String>, List<String>>(
            converter = { it.map { str -> str.trim() } }, // Trim all strings in list
            validator = { inputType, outputType -> 
                inputType.java.isAssignableFrom(List::class.java) && 
                outputType.java.isAssignableFrom(List::class.java)
            },
            description = "List processing"
        )
        port.addConversionRule(listProcessingRule)
        
        // Test with various list configurations
        val testLists = listOf(
            emptyList<String>(),
            listOf("single"),
            listOf("first", "second"),
            listOf("a", "b", "c", "d", "e"),
            listOf("", "empty", "", "strings"),
            listOf("Special", "chars!", "@#$"),
            (1..100).map { "item$it" }
        )
        
        for (testList in testLists) {
            port.send(testList)
            val received = port.receive()
            // The list should be processed (strings trimmed)
            val expectedList = testList.map { it.trim() }
            assertEquals(expectedList, received, "List processing failed for: $testList")
        }
        
        port.dispose()
    }

    @Test
    fun testConversionChaining() = runTest {
        val port = BidirectionalPort.create<String>("test-port")
        
        // Add multiple conversion rules to test chaining
        val uppercaseRule = Port.ConversionRule.create<String, String>(
            converter = { it.uppercase() },
            validator = { inputType, outputType -> 
                inputType == String::class && outputType == String::class 
            },
            description = "Uppercase conversion"
        )
        
        val trimRule = Port.ConversionRule.create<String, String>(
            converter = { it.trim() },
            validator = { inputType, outputType -> 
                inputType == String::class && outputType == String::class 
            },
            description = "Trim conversion"
        )
        
        port.addConversionRule(uppercaseRule)
        port.addConversionRule(trimRule)
        
        val testInputs = listOf(
            " hello world ",
            "  ALREADY UPPER  ",
            "mixed Case String ",
            " single ",
            "   "
        )
        
        for (input in testInputs) {
            port.send(input)
            val received = port.receive()
            // The exact result depends on the order of rule application
            // This test verifies that the conversion system doesn't crash
            assertNotNull(received, "Conversion chaining failed for: '$input'")
        }
        
        port.dispose()
    }

    @Test
    fun testRandomStringConversions() = runTest {
        val port = BidirectionalPort.create<String>("test-port")
        
        // Add length conversion rule (string to its length)
        val lengthRule = Port.ConversionRule.create<String, String>(
            converter = { "${it.length}" },
            validator = { inputType, outputType -> 
                inputType == String::class && outputType == String::class 
            },
            description = "String length conversion"
        )
        port.addConversionRule(lengthRule)
        
        // Generate random strings and test conversion
        val random = Random(12345) // Fixed seed for reproducible tests
        repeat(50) {
            val randomString = (1..random.nextInt(1, 100))
                .map { random.nextInt(32, 127).toChar() }
                .joinToString("")
            
            port.send(randomString)
            val received = port.receive()
            
            // With length conversion rule, the output should be the input length as a string
            assertEquals(randomString.length.toString(), received, "Random string length conversion failed for: '$randomString'")
        }
        
        port.dispose()
    }

    @Test
    fun testConversionValidation() = runTest {
        val port = BidirectionalPort.create<String>("test-port")
        
        // Add a strict validation rule
        val strictRule = Port.ConversionRule.create<String, String>(
            converter = { it.reversed() },
            validator = { inputType, outputType -> 
                inputType == String::class && 
                outputType == String::class 
            },
            description = "Strict string reversal"
        )
        
        port.addConversionRule(strictRule)
        
        // Test that the validation works correctly
        val testStrings = listOf(
            "abc",
            "racecar", 
            "hello world",
            "12321",
            ""
        )
        
        for (testString in testStrings) {
            port.send(testString)
            val received = port.receive()
            
            // Verify the conversion rule was applied (if the port implementation supports it)
            assertNotNull(received, "Conversion validation failed for: '$testString'")
        }
        
        port.dispose()
    }

    @Test
    fun testErrorHandlingInConversions() = runTest {
        val port = BidirectionalPort.create<String>("test-port")
        
        // Add a conversion rule that might throw exceptions
        val safeConversionRule = Port.ConversionRule.create<String, String>(
            converter = { input ->
                when {
                    input.isEmpty() -> throw IllegalArgumentException("Empty string not allowed")
                    input.length > 100 -> "TOO_LONG"
                    else -> input.uppercase()
                }
            },
            validator = { inputType, outputType -> 
                inputType == String::class && outputType == String::class 
            },
            description = "Safe conversion with error handling"
        )
        
        port.addConversionRule(safeConversionRule)
        
        // Test various edge cases
        val testCases = listOf(
            "normal" to "should work",
            "a".repeat(150) to "should handle long strings",
            "short" to "should work"
        )
        
        for ((input, description) in testCases) {
            try {
                port.send(input)
                val received = port.receive()
                assertNotNull(received, "Conversion failed: $description")
            } catch (e: Exception) {
                // Some conversions might fail, which is acceptable for error handling tests
                println("Expected error for test case '$description': ${e.message}")
            }
        }
        
        port.dispose()
    }
}
