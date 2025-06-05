package ai.solace.core.actor.examples

import kotlinx.coroutines.test.runTest
import kotlin.test.*

class FilterTest {

    @Test
    fun testFilterInitialization() = runTest {
        // Create a Filter actor
        val filter = Filter(
            name = "TestFilter",
            predicate = { it.length > 5 },
            includeRejectedPort = true,
            messageClass = String::class
        )

        // Initialize the filter
        filter.initialize()

        // Verify the filter has the expected ports
        val inputPort = filter.getPort(Filter.INPUT_PORT, String::class)
        val acceptedPort = filter.getPort(Filter.ACCEPTED_PORT, String::class)
        val rejectedPort = filter.getPort(Filter.REJECTED_PORT, String::class)

        assertNotNull(inputPort, "Input port should be created")
        assertNotNull(acceptedPort, "Accepted port should be created")
        assertNotNull(rejectedPort, "Rejected port should be created when includeRejectedPort is true")
    }

    @Test
    fun testFilterWithoutRejectedPort() = runTest {
        // Create a Filter actor without a rejected port
        val filter = Filter(
            name = "TestFilter",
            predicate = { it.length > 5 },
            includeRejectedPort = false,
            messageClass = String::class
        )

        // Initialize the filter
        filter.initialize()

        // Verify the filter has only input and accepted ports
        val inputPort = filter.getPort(Filter.INPUT_PORT, String::class)
        val acceptedPort = filter.getPort(Filter.ACCEPTED_PORT, String::class)
        val rejectedPort = filter.getPort(Filter.REJECTED_PORT, String::class)

        assertNotNull(inputPort, "Input port should be created")
        assertNotNull(acceptedPort, "Accepted port should be created")
        assertEquals(null, rejectedPort, "Rejected port should not be created when includeRejectedPort is false")
    }

    @Test
    fun testFilterPredicate() = runTest {
        // Test the predicate directly
        val predicate: (String) -> Boolean = { it.length > 5 }

        // Test with a string longer than 5 characters
        val longString = "This is a long string"
        assertTrue(predicate(longString), "Predicate should return true for strings longer than 5 characters")

        // Test with a string shorter than or equal to 5 characters
        val shortString = "Short"
        assertFalse(predicate(shortString), "Predicate should return false for strings shorter than or equal to 5 characters")
    }

    @Test
    fun testFilterWithRejectedPort() = runTest {
        // Create a Filter actor with a rejected port
        val filter = Filter(
            name = "TestFilter",
            predicate = { it.length > 5 },
            includeRejectedPort = true,
            messageClass = String::class
        )

        // Initialize the filter
        filter.initialize()

        // Verify the filter has all three ports
        val inputPort = filter.getPort(Filter.INPUT_PORT, String::class)
        val acceptedPort = filter.getPort(Filter.ACCEPTED_PORT, String::class)
        val rejectedPort = filter.getPort(Filter.REJECTED_PORT, String::class)

        assertNotNull(inputPort, "Input port should be created")
        assertNotNull(acceptedPort, "Accepted port should be created")
        assertNotNull(rejectedPort, "Rejected port should be created when includeRejectedPort is true")
    }

    @Test
    fun testCustomPredicate() = runTest {
        // Test a custom predicate directly
        val predicate: (String) -> Boolean = { it.toIntOrNull() != null }

        // Test with a string that can be parsed as an integer
        val validInput = "123"
        assertTrue(predicate(validInput), "Predicate should return true for strings that can be parsed as integers")

        // Test with a string that cannot be parsed as an integer
        val invalidInput = "abc"
        assertFalse(predicate(invalidInput), "Predicate should return false for strings that cannot be parsed as integers")
    }

    @Test
    fun testFilterStartInitializesIfNeeded() = runTest {
        // Create a Filter actor
        val filter = Filter(
            name = "TestFilter",
            predicate = { it.length > 5 },
            includeRejectedPort = true,
            messageClass = String::class
        )

        // Start the filter without explicitly initializing it
        filter.start()

        // Verify the filter has been initialized
        val inputPort = filter.getPort(Filter.INPUT_PORT, String::class)
        val acceptedPort = filter.getPort(Filter.ACCEPTED_PORT, String::class)
        val rejectedPort = filter.getPort(Filter.REJECTED_PORT, String::class)

        assertNotNull(inputPort, "Input port should be created during start")
        assertNotNull(acceptedPort, "Accepted port should be created during start")
        assertNotNull(rejectedPort, "Rejected port should be created during start when includeRejectedPort is true")
    }

    @Test
    fun testFilterDispose() = runTest {
        // Create a Filter actor
        val filter = Filter(
            name = "TestFilter",
            predicate = { it.length > 5 },
            includeRejectedPort = true,
            messageClass = String::class
        )

        // Initialize and start the filter
        filter.initialize()
        filter.start()

        // Verify the filter is active
        assertTrue(filter.isActive())

        // Dispose the filter
        filter.dispose()

        // Verify the filter is no longer active
        assertFalse(filter.isActive())
    }
}
