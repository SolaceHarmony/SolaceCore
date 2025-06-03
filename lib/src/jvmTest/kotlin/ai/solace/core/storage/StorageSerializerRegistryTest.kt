package ai.solace.core.storage

import kotlin.test.*

class StorageSerializerRegistryTest {
    
    // Test data class
    data class TestData(val name: String, val value: Int)
    
    // Test serializer for TestData
    class TestDataSerializer : StorageSerializer<TestData> {
        override fun serialize(obj: TestData): Map<String, Any> {
            return mapOf(
                "name" to obj.name,
                "value" to obj.value
            )
        }
        
        override fun deserialize(map: Map<String, Any>): TestData {
            return TestData(
                name = map["name"] as String,
                value = map["value"] as Int
            )
        }
    }
    
    @BeforeTest
    fun setup() {
        // Clear any existing serializers
        // Note: StorageSerializerRegistry doesn't have a clear method, so we'll just
        // make sure our test doesn't depend on existing state
    }
    
    @Test
    fun testRegisterAndGetSerializer() {
        // Register a serializer
        val serializer = TestDataSerializer()
        StorageSerializerRegistry.registerSerializer(TestData::class.java, serializer)
        
        // Get the serializer
        val retrievedSerializer = StorageSerializerRegistry.getSerializer(TestData::class.java)
        assertNotNull(retrievedSerializer)
        assertTrue(retrievedSerializer is TestDataSerializer)
    }
    
    @Test
    fun testSerializeAndDeserialize() {
        // Register a serializer
        val serializer = TestDataSerializer()
        StorageSerializerRegistry.registerSerializer(TestData::class.java, serializer)
        
        // Create a test object
        val testData = TestData("test", 42)
        
        // Serialize the object
        val serialized = StorageSerializerRegistry.serialize(testData)
        assertNotNull(serialized)
        assertEquals("test", serialized["name"])
        assertEquals(42, serialized["value"])
        
        // Deserialize the map
        val deserialized = StorageSerializerRegistry.deserialize(serialized, TestData::class.java)
        assertNotNull(deserialized)
        assertEquals(testData, deserialized)
    }
    
    @Test
    fun testSerializeUnregisteredType() {
        // Create a test object of an unregistered type
        class UnregisteredType(val data: String)
        val unregisteredObject = UnregisteredType("test")
        
        // Try to serialize it
        val serialized = StorageSerializerRegistry.serialize(unregisteredObject)
        assertNull(serialized)
    }
    
    @Test
    fun testDeserializeUnregisteredType() {
        // Create a map to deserialize
        val map = mapOf("data" to "test")
        
        // Try to deserialize it to an unregistered type
        class UnregisteredType(val data: String)
        val deserialized = StorageSerializerRegistry.deserialize(map, UnregisteredType::class.java)
        assertNull(deserialized)
    }
    
    @Test
    fun testMultipleSerializers() {
        // Define another test class and serializer
        data class AnotherTestData(val id: Long, val active: Boolean)
        
        class AnotherTestDataSerializer : StorageSerializer<AnotherTestData> {
            override fun serialize(obj: AnotherTestData): Map<String, Any> {
                return mapOf(
                    "id" to obj.id,
                    "active" to obj.active
                )
            }
            
            override fun deserialize(map: Map<String, Any>): AnotherTestData {
                return AnotherTestData(
                    id = map["id"] as Long,
                    active = map["active"] as Boolean
                )
            }
        }
        
        // Register both serializers
        StorageSerializerRegistry.registerSerializer(TestData::class.java, TestDataSerializer())
        StorageSerializerRegistry.registerSerializer(AnotherTestData::class.java, AnotherTestDataSerializer())
        
        // Create test objects
        val testData = TestData("test", 42)
        val anotherTestData = AnotherTestData(123L, true)
        
        // Serialize both objects
        val serialized1 = StorageSerializerRegistry.serialize(testData)
        val serialized2 = StorageSerializerRegistry.serialize(anotherTestData)
        
        assertNotNull(serialized1)
        assertNotNull(serialized2)
        
        // Deserialize both maps
        val deserialized1 = StorageSerializerRegistry.deserialize(serialized1, TestData::class.java)
        val deserialized2 = StorageSerializerRegistry.deserialize(serialized2, AnotherTestData::class.java)
        
        assertNotNull(deserialized1)
        assertNotNull(deserialized2)
        
        assertEquals(testData, deserialized1)
        assertEquals(anotherTestData, deserialized2)
    }
    
    @Test
    fun testOverwriteSerializer() {
        // Register a serializer
        val serializer1 = TestDataSerializer()
        StorageSerializerRegistry.registerSerializer(TestData::class.java, serializer1)
        
        // Create a different serializer that adds a prefix to the name
        class PrefixedTestDataSerializer : StorageSerializer<TestData> {
            override fun serialize(obj: TestData): Map<String, Any> {
                return mapOf(
                    "name" to "prefix_${obj.name}",
                    "value" to obj.value
                )
            }
            
            override fun deserialize(map: Map<String, Any>): TestData {
                val name = (map["name"] as String).removePrefix("prefix_")
                return TestData(name, map["value"] as Int)
            }
        }
        
        // Register the new serializer, overwriting the old one
        val serializer2 = PrefixedTestDataSerializer()
        StorageSerializerRegistry.registerSerializer(TestData::class.java, serializer2)
        
        // Create a test object
        val testData = TestData("test", 42)
        
        // Serialize using the new serializer
        val serialized = StorageSerializerRegistry.serialize(testData)
        assertNotNull(serialized)
        assertEquals("prefix_test", serialized["name"])
        
        // Deserialize using the new serializer
        val deserialized = StorageSerializerRegistry.deserialize(serialized, TestData::class.java)
        assertNotNull(deserialized)
        assertEquals(testData, deserialized)
    }
}