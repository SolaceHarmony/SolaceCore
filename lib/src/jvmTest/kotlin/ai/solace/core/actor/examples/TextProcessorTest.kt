package ai.solace.core.actor.examples

import ai.solace.core.actor.Actor
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class TextProcessorTest {

    @Test
    fun testTextProcessorInitialization() = runTest {
        // Create a TextProcessor actor
        val textProcessor = TextProcessor(
            name = "TestTextProcessor",
            transformations = listOf(TextProcessor.TO_UPPERCASE)
        )

        // Initialize the text processor
        textProcessor.initialize()

        // Verify the text processor has the expected ports
        val inputPort = textProcessor.getPort(TextProcessor.INPUT_PORT, String::class)
        val outputPort = textProcessor.getPort(TextProcessor.OUTPUT_PORT, String::class)

        assertNotNull(inputPort, "Input port should be created")
        assertNotNull(outputPort, "Output port should be created")
    }

    @Test
    fun testToUppercaseTransformation() = runTest {
        // Test the TO_UPPERCASE transformation directly
        val transformation = TextProcessor.TO_UPPERCASE

        // Test with a simple string
        val input = "Hello, World!"
        val output = transformation(input)

        // Verify the transformation
        assertEquals("HELLO, WORLD!", output)
    }

    @Test
    fun testToLowercaseTransformation() = runTest {
        // Test the TO_LOWERCASE transformation directly
        val transformation = TextProcessor.TO_LOWERCASE

        // Test with a simple string
        val input = "Hello, World!"
        val output = transformation(input)

        // Verify the transformation
        assertEquals("hello, world!", output)
    }

    @Test
    fun testTrimTransformation() = runTest {
        // Test the TRIM transformation directly
        val transformation = TextProcessor.TRIM

        // Test with a string that has whitespace at the beginning and end
        val input = "   Hello, World!   "
        val output = transformation(input)

        // Verify the transformation
        assertEquals("Hello, World!", output)
    }

    @Test
    fun testRemoveWhitespaceTransformation() = runTest {
        // Test the REMOVE_WHITESPACE transformation directly
        val transformation = TextProcessor.REMOVE_WHITESPACE

        // Test with a string that has whitespace
        val input = "Hello, World! This has spaces."
        val output = transformation(input)

        // Verify the transformation
        assertEquals("Hello,World!Thishasspaces.", output)
    }

    @Test
    fun testReverseTransformation() = runTest {
        // Test the REVERSE transformation directly
        val transformation = TextProcessor.REVERSE

        // Test with a simple string
        val input = "Hello, World!"
        val output = transformation(input)

        // Verify the transformation
        assertEquals("!dlroW ,olleH", output)
    }

    @Test
    fun testMultipleTransformations() = runTest {
        // Create a list of transformations
        val transformations = listOf(
            TextProcessor.TRIM,
            TextProcessor.TO_UPPERCASE,
            TextProcessor.REMOVE_WHITESPACE
        )

        // Apply the transformations in sequence
        var result = "   Hello, World!   "
        for (transformation in transformations) {
            result = transformation(result)
        }

        // Verify all transformations were applied in sequence:
        // 1. TRIM: "Hello, World!"
        // 2. TO_UPPERCASE: "HELLO, WORLD!"
        // 3. REMOVE_WHITESPACE: "HELLO,WORLD!"
        assertEquals("HELLO,WORLD!", result)
    }

    @Test
    fun testCustomTransformation() = runTest {
        // Create a custom transformation
        val replaceVowels: (String) -> String = { it.replace("[aeiouAEIOU]".toRegex(), "*") }

        // Test with a simple string
        val input = "Hello, World!"
        val output = replaceVowels(input)

        // Verify the custom transformation
        assertEquals("H*ll*, W*rld!", output)
    }

    @Test
    fun testTextProcessorStartInitializesIfNeeded() = runTest {
        // Create a TextProcessor actor
        val textProcessor = TextProcessor(
            name = "TestTextProcessor",
            transformations = listOf(TextProcessor.TO_UPPERCASE)
        )

        // Start the text processor without explicitly initializing it
        textProcessor.start()

        // Verify the text processor has been initialized
        val inputPort = textProcessor.getPort(TextProcessor.INPUT_PORT, String::class)
        val outputPort = textProcessor.getPort(TextProcessor.OUTPUT_PORT, String::class)

        assertNotNull(inputPort, "Input port should be created during start")
        assertNotNull(outputPort, "Output port should be created during start")
    }

    @Test
    fun testTextProcessorDispose() = runTest {
        // Create a TextProcessor actor
        val textProcessor = TextProcessor(
            name = "TestTextProcessor",
            transformations = listOf(TextProcessor.TO_UPPERCASE)
        )

        // Initialize and start the text processor
        textProcessor.initialize()
        textProcessor.start()

        // Verify the text processor is active
        assertTrue(textProcessor.isActive())

        // Dispose the text processor
        textProcessor.dispose()

        // Verify the text processor is no longer active
        assertFalse(textProcessor.isActive())
    }
}
