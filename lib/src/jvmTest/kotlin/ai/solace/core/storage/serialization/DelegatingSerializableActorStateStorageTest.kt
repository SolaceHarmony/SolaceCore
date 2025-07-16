package ai.solace.core.storage.serialization

import ai.solace.core.actor.ActorState
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class DelegatingSerializableActorStateStorageTest {

    private lateinit var storage: DelegatingSerializableActorStateStorage

    @BeforeTest
    fun setup() {
        storage = DelegatingSerializableActorStateStorage.createInMemory()
    }

    @Test
    fun testGetSerializerRegistry() {
        // Verify that the serializer registry is not null
        assertNotNull(storage.getSerializerRegistry())

        // Verify that the ActorStateEnumSerializer is registered
        val serializer = storage.getSerializerRegistry().getSerializer(ActorState::class)
        assertNotNull(serializer)
        assertTrue(serializer is ActorStateEnumSerializer)
    }

    @Test
    fun testSerializeAndStoreActorState() = runBlocking {
        // Create an actor state to serialize and store
        val actorId = "test-actor"
        val state = ActorState.Running

        // Serialize and store the actor state
        val result = storage.serializeAndStore(actorId, "state", state, ActorState::class)
        assertTrue(result)

        // Retrieve and deserialize the actor state
        val retrievedState = storage.retrieveAndDeserialize(actorId, "state", ActorState::class)
        assertNotNull(retrievedState)
        assertEquals(state, retrievedState)
    }

    @Test
    fun testSerializeAndStoreCustomObject() = runBlocking {
        // Create a custom serializer
        val customSerializer = object : ActorStateSerializer<TestData> {
            override fun serialize(obj: TestData): Map<String, Any> {
                return mapOf("name" to obj.name, "value" to obj.value)
            }

            override fun deserialize(map: Map<String, Any>): TestData? {
                val name = map["name"] as? String ?: return null
                val value = map["value"] as? Int ?: return null
                return TestData(name, value)
            }

            override fun getType(): kotlin.reflect.KClass<TestData> {
                return TestData::class
            }
        }

        // Register the custom serializer
        storage.getSerializerRegistry().registerSerializer(customSerializer)

        // Create a custom object to serialize and store
        val actorId = "test-actor"
        val data = TestData("test", 42)

        // Serialize and store the custom object
        val result = storage.serializeAndStore(actorId, "custom", data, TestData::class)
        assertTrue(result)

        // Retrieve and deserialize the custom object
        val retrievedData = storage.retrieveAndDeserialize(actorId, "custom", TestData::class)
        assertNotNull(retrievedData)
        assertEquals(data.name, retrievedData.name)
        assertEquals(data.value, retrievedData.value)
    }

    @Test
    fun testDelegation() = runBlocking {
        // Test that the storage delegates to the underlying ActorStateStorage
        val actorId = "test-actor"
        val state = ActorState.Running

        // Set actor state
        assertTrue(storage.setActorState(actorId, state))

        // Get actor state
        val retrievedState = storage.getActorState(actorId)
        assertNotNull(retrievedState)
        assertEquals(state, retrievedState)

        // Set actor ports
        val ports = mapOf("input" to mapOf("type" to "String"))
        assertTrue(storage.setActorPorts(actorId, ports))

        // Get actor ports
        val retrievedPorts = storage.getActorPorts(actorId)
        assertNotNull(retrievedPorts)
        assertEquals(ports, retrievedPorts)

        // Set actor metrics
        val metrics = mapOf("count" to 42)
        assertTrue(storage.setActorMetrics(actorId, metrics))

        // Get actor metrics
        val retrievedMetrics = storage.getActorMetrics(actorId)
        assertNotNull(retrievedMetrics)
        assertEquals(metrics, retrievedMetrics)

        // Set actor custom state
        val customState = mapOf("key" to "value")
        assertTrue(storage.setActorCustomState(actorId, customState))

        // Get actor custom state
        val retrievedCustomState = storage.getActorCustomState(actorId)
        assertNotNull(retrievedCustomState)
        assertEquals(customState, retrievedCustomState)
    }

    @Test
    fun testSerializerNotFound() {
        // Test that an exception is thrown when a serializer is not found
        val actorId = "test-actor"
        val data = TestData("test", 42)

        runBlocking {
            // Attempt to serialize and store without registering a serializer
            assertFailsWith<IllegalArgumentException> {
                storage.serializeAndStore(actorId, "custom", data, TestData::class)
            }

            // Attempt to retrieve and deserialize without registering a serializer
            assertFailsWith<IllegalArgumentException> {
                storage.retrieveAndDeserialize(actorId, "custom", TestData::class)
            }
        }
    }

    /**
     * Test data class for serialization tests.
     */
    data class TestData(val name: String, val value: Int)
}
