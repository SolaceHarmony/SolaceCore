package ai.solace.core.storage.serialization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class SerializationWrapperTest {
    
    @Test
    fun testSerializationWrapper() {
        // Create a SerializationWrapper instance
        val wrapper = SerializationWrapper("test value")
        
        // Verify the value property
        assertEquals("test value", wrapper.value)
        
        // Test equality
        val wrapper2 = SerializationWrapper("test value")
        assertEquals(wrapper, wrapper2)
        
        // Test copy
        val wrapper3 = wrapper.copy(value = "new value")
        assertEquals("new value", wrapper3.value)
    }
    
    @Test
    fun testSerialization() {
        // Create a SerializationWrapper instance
        val wrapper = SerializationWrapper("test value")
        
        // Serialize to JSON
        val json = Json.encodeToString(wrapper)
        
        // Deserialize from JSON
        val deserializedWrapper = Json.decodeFromString<SerializationWrapper>(json)
        
        // Verify the deserialized instance
        assertEquals(wrapper, deserializedWrapper)
        assertEquals("test value", deserializedWrapper.value)
    }
}