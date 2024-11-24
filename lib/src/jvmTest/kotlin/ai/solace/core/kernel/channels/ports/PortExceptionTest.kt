package ai.solace.core.kernel.channels.ports

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class PortExceptionTest {
    @Test
    fun `test port connection with incompatible conversion rules`() = runTest {
        val sourcePort = BidirectionalPort<Int>(
            name = "source",
            type = Int::class
        )

        val targetPort = BidirectionalPort<String>(
            name = "target",
            type = String::class
        )

        val invalidRule = Port.ConversionRule.create<Int, String>(
            converter = { it.toString() },
            validator = { _, _ -> false },
            description = "Invalid conversion"
        )

        val exception = assertFailsWith<PortConnectionException> {
            Port.connect(
                source = sourcePort,
                target = targetPort,
                rules = listOf(invalidRule)
            )
        }

        assertNotNull(exception.message)
        assertTrue(exception.message!!.contains("Incompatible port types"))
        assertTrue(exception.message!!.contains("Invalid conversion"))
    }

    @Test
    fun `test conversion rule chain validation`() = runTest {
        val sourcePort = BidirectionalPort<String>(
            name = "source",
            type = String::class
        )

        val targetPort = BidirectionalPort<Int>(
            name = "target",
            type = Int::class
        )

        val rules = listOf(
            Port.ConversionRule.create<String, Int>(
                converter = { it.toIntOrNull() ?: 0 },
                validator = { input, output ->
                    input == String::class && output == Int::class
                }
            )
        )

        // This should work
        val connection = Port.connect(
            source = sourcePort,
            target = targetPort,
            rules = rules
        )

        assertNotNull(connection)
    }
}