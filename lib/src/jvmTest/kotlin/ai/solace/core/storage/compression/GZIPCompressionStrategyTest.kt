package ai.solace.core.storage.compression

import kotlin.test.*

/**
 * Tests for the GZIPCompressionStrategy class.
 */
class GZIPCompressionStrategyTest {
    private lateinit var strategy: GZIPCompressionStrategy

    @BeforeTest
    fun setup() {
        strategy = GZIPCompressionStrategy()
    }

    @Test
    fun `test compress and decompress`() {
        // Create a test string
        val original = "This is a test string that will be compressed and decompressed. " +
                "Adding more text to ensure it's large enough to benefit from compression. " +
                "GZIP works best with repetitive content, so let's repeat this a few times. " +
                "This is a test string that will be compressed and decompressed. " +
                "This is a test string that will be compressed and decompressed. " +
                "This is a test string that will be compressed and decompressed."
        val originalBytes = original.toByteArray()

        println("[DEBUG_LOG] Original size: ${originalBytes.size}")

        // Compress the string
        val compressed = strategy.compress(originalBytes)

        println("[DEBUG_LOG] Compressed size: ${compressed.size}")

        // Verify that the compressed data is smaller than the original
        assertTrue(compressed.size < originalBytes.size)

        // Decompress the string
        val decompressed = strategy.decompress(compressed)

        // Verify that the decompressed data matches the original
        assertContentEquals(originalBytes, decompressed)
        assertEquals(original, String(decompressed))
    }

    @Test
    fun `test compress and decompress large data`() {
        // Create a large test string
        val original = "A".repeat(10000)
        val originalBytes = original.toByteArray()

        // Compress the string
        val compressed = strategy.compress(originalBytes)

        // Verify that the compressed data is much smaller than the original
        assertTrue(compressed.size < originalBytes.size / 10)

        // Decompress the string
        val decompressed = strategy.decompress(compressed)

        // Verify that the decompressed data matches the original
        assertContentEquals(originalBytes, decompressed)
        assertEquals(original, String(decompressed))
    }

    @Test
    fun `test serialize and deserialize string`() {
        // Create a test string
        val original = "This is a test string that will be serialized and deserialized."

        // Serialize the string
        val serialized = strategy.serialize(original)

        // Deserialize the string
        val deserialized = strategy.deserialize(serialized, String::class.java)

        // Verify that the deserialized data matches the original
        assertEquals(original, deserialized)
    }

    @Test
    fun `test serialize and deserialize primitive types`() {
        // Test with various primitive types
        val intValue = 42
        val longValue = 42L
        val floatValue = 42.0f
        val doubleValue = 42.0
        val booleanValue = true

        // Serialize and deserialize int
        val intSerialized = strategy.serialize(intValue)
        val intDeserialized = strategy.deserialize(intSerialized, Int::class.java)
        assertEquals(intValue, intDeserialized)

        // Serialize and deserialize long
        val longSerialized = strategy.serialize(longValue)
        val longDeserialized = strategy.deserialize(longSerialized, Long::class.java)
        assertEquals(longValue, longDeserialized)

        // Serialize and deserialize float
        val floatSerialized = strategy.serialize(floatValue)
        val floatDeserialized = strategy.deserialize(floatSerialized, Float::class.java)
        assertEquals(floatValue, floatDeserialized)

        // Serialize and deserialize double
        val doubleSerialized = strategy.serialize(doubleValue)
        val doubleDeserialized = strategy.deserialize(doubleSerialized, Double::class.java)
        assertEquals(doubleValue, doubleDeserialized)

        // Serialize and deserialize boolean
        val booleanSerialized = strategy.serialize(booleanValue)
        val booleanDeserialized = strategy.deserialize(booleanSerialized, Boolean::class.java)
        assertEquals(booleanValue, booleanDeserialized)
    }

    @Test
    fun `test serialize and deserialize map`() {
        // Create a test map
        val original = mapOf(
            "key1" to "value1",
            "key2" to 42,
            "key3" to true
        )

        // Serialize the map
        val serialized = strategy.serialize(original)

        // Deserialize the map
        @Suppress("UNCHECKED_CAST")
        val deserialized = strategy.deserialize(serialized, Map::class.java) as Map<String, Any>

        // Verify that the deserialized data contains the expected keys and values
        assertEquals(3, deserialized.size)
        assertEquals("value1", deserialized["key1"])
        assertEquals(42.0, deserialized["key2"]) // Note: Numbers are deserialized as Double in JSON
        assertEquals(true, deserialized["key3"])
    }

    @Test
    fun `test serialize and deserialize byte array`() {
        // Create a test byte array
        val original = byteArrayOf(1, 2, 3, 4, 5)

        // Serialize the byte array
        val serialized = strategy.serialize(original)

        // Deserialize the byte array
        val deserialized = strategy.deserialize(serialized, ByteArray::class.java)

        // Verify that the deserialized data matches the original
        assertContentEquals(original, deserialized)
    }
}
