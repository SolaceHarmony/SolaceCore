package ai.solace.core.storage.serialization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.reflect.KClass

class ActorStateSerializerRegistryTest {
    
    // Test data class
    data class TestData(val name: String, val value: Int)
    
    // Test serializer for TestData
    class TestDataSerializer : ActorStateSerializer<TestData> {
        override fun serialize(obj: TestData): Map<String, Any> {
            return mapOf(
                "name" to obj.name,
                "value" to obj.value
            )
        }
        
        override fun deserialize(map: Map<String, Any>): TestData? {
            return TestData(
                name = map["name"] as String,
                value = map["value"] as Int
            )
        }
        
        override fun getType(): KClass<TestData> {
            return TestData::class
        }
    }
    
    @Test
    fun testRegisterAndGetSerializer() {
        // Create a registry
        val registry = ActorStateSerializerRegistry()
        
        // Register a serializer
        val serializer = TestDataSerializer()
        registry.registerSerializer(serializer)
        
        // Get the serializer
        val retrievedSerializer = registry.getSerializer(TestData::class)
        
        // Verify the serializer was retrieved correctly
        assertNotNull(retrievedSerializer)
        assertTrue(retrievedSerializer is TestDataSerializer)
    }
    
    @Test
    fun testHasSerializer() {
        // Create a registry
        val registry = ActorStateSerializerRegistry()
        
        // Initially, the registry should not have a serializer for TestData
        assertFalse(registry.hasSerializer(TestData::class))
        
        // Register a serializer
        val serializer = TestDataSerializer()
        registry.registerSerializer(serializer)
        
        // Now the registry should have a serializer for TestData
        assertTrue(registry.hasSerializer(TestData::class))
    }
    
    @Test
    fun testUnregisterSerializer() {
        // Create a registry
        val registry = ActorStateSerializerRegistry()
        
        // Register a serializer
        val serializer = TestDataSerializer()
        registry.registerSerializer(serializer)
        
        // Verify the serializer is registered
        assertTrue(registry.hasSerializer(TestData::class))
        
        // Unregister the serializer
        val result = registry.unregisterSerializer(TestData::class)
        
        // Verify the serializer was unregistered
        assertTrue(result)
        assertFalse(registry.hasSerializer(TestData::class))
        
        // Trying to unregister again should return false
        val result2 = registry.unregisterSerializer(TestData::class)
        assertFalse(result2)
    }
    
    @Test
    fun testGetAllSerializers() {
        // Create a registry
        val registry = ActorStateSerializerRegistry()
        
        // Initially, the registry should have no serializers
        assertTrue(registry.getAllSerializers().isEmpty())
        
        // Register a serializer
        val serializer = TestDataSerializer()
        registry.registerSerializer(serializer)
        
        // Get all serializers
        val allSerializers = registry.getAllSerializers()
        
        // Verify the serializers
        assertEquals(1, allSerializers.size)
        assertTrue(allSerializers.containsKey(TestData::class))
        assertTrue(allSerializers[TestData::class] is TestDataSerializer)
    }
    
    @Test
    fun testSerializeAndDeserialize() {
        // Create a registry
        val registry = ActorStateSerializerRegistry()
        
        // Register a serializer
        val serializer = TestDataSerializer()
        registry.registerSerializer(serializer)
        
        // Create a test object
        val testData = TestData("test", 42)
        
        // Get the serializer and use it to serialize the object
        val retrievedSerializer = registry.getSerializer(TestData::class)
        assertNotNull(retrievedSerializer)
        
        val serialized = retrievedSerializer.serialize(testData)
        
        // Verify the serialized data
        assertEquals("test", serialized["name"])
        assertEquals(42, serialized["value"])
        
        // Deserialize the data
        val deserialized = retrievedSerializer.deserialize(serialized)
        
        // Verify the deserialized object
        assertNotNull(deserialized)
        assertEquals(testData, deserialized)
    }
    
    @Test
    fun testGetSerializerForUnregisteredType() {
        // Create a registry
        val registry = ActorStateSerializerRegistry()
        
        // Try to get a serializer for an unregistered type
        val serializer = registry.getSerializer(TestData::class)
        
        // Verify that no serializer was found
        assertNull(serializer)
    }
}