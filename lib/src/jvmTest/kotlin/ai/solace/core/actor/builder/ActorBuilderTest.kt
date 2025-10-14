package ai.solace.core.actor.builder

import ai.solace.core.actor.Actor
import ai.solace.core.workflow.WorkflowManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ActorBuilderTest {

    @Test
    fun testBuildActorNetworkAsync() = runTest {
        // Create an ActorBuilder using the buildActorNetworkAsync function
        val builder = buildActorNetworkAsync()

        // Verify the builder is not null
        assertNotNull(builder)

        // Build the workflow manager
        val workflowManager = builder.build()

        // Verify the workflow manager is not null
        assertNotNull(workflowManager)
        assertTrue(workflowManager is WorkflowManager)
    }

    @Test
    @Suppress("DEPRECATION")
    fun testBuildActorNetwork() {
        // Create an ActorBuilder using the deprecated buildActorNetwork function
        val builder = buildActorNetwork()

        // Verify the builder is not null
        assertNotNull(builder)

        // Build the workflow manager
        val workflowManager = builder.build()

        // Verify the workflow manager is not null
        assertNotNull(workflowManager)
        assertTrue(workflowManager is WorkflowManager)
    }

    @Test
    fun testAddActor() = runTest {
        // Create an ActorBuilder
        val builder = buildActorNetworkAsync()

        // Create a test actor
        val actor = TestActor("test-actor")

        // Add the actor to the builder
        val result = builder.addActor(actor)

        // Verify the builder is returned for method chaining
        assertEquals(builder, result)

        // Build the workflow manager
        builder.build()

        // Verify the actor was added to the workflow manager
        // Note: This assumes the WorkflowManager has a way to check if an actor was added
        // If not, we might need to modify this test or add a method to WorkflowManager
    }

    @Test
    @Suppress("DEPRECATION")
    fun testAddActorBlocking() {
        // Create an ActorBuilder
        val builder = buildActorNetwork()

        // Create a test actor
        val actor = TestActor("test-actor")

        // Add the actor to the builder using the deprecated blocking method
        val result = builder.addActorBlocking(actor)

        // Verify the builder is returned for method chaining
        assertEquals(builder, result)

        // Build the workflow manager
        builder.build()

        // Verify the actor was added to the workflow manager
        // Note: This assumes the WorkflowManager has a way to check if an actor was added
        // If not, we might need to modify this test or add a method to WorkflowManager
    }

    @Test
    fun testConnect() = runTest {
        // Create an ActorBuilder
        val builder = buildActorNetworkAsync()

        // Create test actors
        val sourceActor = TestActor("source-actor")
        val targetActor = TestActor("target-actor")

        // Add the actors to the builder with port definitions
        builder.addActor(sourceActor, mapOf("output" to String::class))
        builder.addActor(targetActor, mapOf("input" to String::class))

        // Connect the actors' ports
        val result = builder.connect(sourceActor, "output", targetActor, "input")

        // Verify the builder is returned for method chaining
        assertEquals(builder, result)

        // Build the workflow manager
        builder.build()

        // Verify the actors are connected in the workflow manager
        // Note: This assumes the WorkflowManager has a way to check if actors are connected
        // If not, we might need to modify this test or add a method to WorkflowManager
    }

    @Test
    @Suppress("DEPRECATION")
    fun testConnectBlocking() {
        // Create an ActorBuilder
        val builder = buildActorNetwork()

        // Create test actors
        val sourceActor = TestActor("source-actor")
        val targetActor = TestActor("target-actor")

        // Add the actors to the builder with port definitions
        builder.addActorBlocking(sourceActor, mapOf("output" to String::class))
        builder.addActorBlocking(targetActor, mapOf("input" to String::class))

        // Connect the actors' ports using the deprecated blocking method
        val result = builder.connectBlocking(sourceActor, "output", targetActor, "input")

        // Verify the builder is returned for method chaining
        assertEquals(builder, result)

        // Build the workflow manager
        builder.build()

        // Verify the actors are connected in the workflow manager
        // Note: This assumes the WorkflowManager has a way to check if actors are connected
        // If not, we might need to modify this test or add a method to WorkflowManager
    }

    @Test
    fun testConnectWithIncompatibleTypes() = runTest {
        // Create an ActorBuilder
        val builder = buildActorNetworkAsync()

        // Create test actors
        val sourceActor = TestActor("source-actor")
        val targetActor = TestActor("target-actor")

        // Add the actors to the builder with incompatible port definitions
        builder.addActor(sourceActor, mapOf("output" to String::class))
        builder.addActor(targetActor, mapOf("input" to Int::class))

        // Attempt to connect the actors' ports with incompatible types
        assertFailsWith<IllegalArgumentException> {
            builder.connect(sourceActor, "output", targetActor, "input")
        }
    }

    // Test implementation of Actor
    private class TestActor(actorName: String) : Actor(
        id = actorName,
        name = actorName
    ) {
        // No additional implementation needed for testing
    }
    
    // ===== ENHANCED CONNECTION TESTS =====
    
    @Test
    fun testConnectMultipleActors() = runTest {
        val builder = buildActorNetworkAsync()
        
        val actor1 = TestActor("actor1")
        val actor2 = TestActor("actor2")
        val actor3 = TestActor("actor3")
        
        // Add actors with port definitions
        builder.addActor(actor1, mapOf("output" to String::class))
        builder.addActor(actor2, mapOf("input" to String::class, "output" to String::class))
        builder.addActor(actor3, mapOf("input" to String::class))
        
        // Create a chain: actor1 -> actor2 -> actor3
        builder.connect(actor1, "output", actor2, "input")
        builder.connect(actor2, "output", actor3, "input")
        
        val workflowManager = builder.build()
        assertNotNull(workflowManager)
    }
    
    @Test
    fun testAddActorWithComplexPortDefinitions() = runTest {
        val builder = buildActorNetworkAsync()
        val actor = TestActor("complex-actor")
        
        val portDefinitions = mapOf(
            "stringInput" to String::class,
            "intInput" to Int::class,
            "stringOutput" to String::class,
            "intOutput" to Int::class
        )
        
        val result = builder.addActor(actor, portDefinitions)
        assertEquals(builder, result)
        
        val workflowManager = builder.build()
        assertNotNull(workflowManager)
    }
    
    @Test
    fun testConnectActorsWithSamePortType() = runTest {
        val builder = buildActorNetworkAsync()
        
        val producer = TestActor("producer")
        val consumer1 = TestActor("consumer1")
        val consumer2 = TestActor("consumer2")
        
        // Add actors with same port types
        builder.addActor(producer, mapOf("output" to String::class))
        builder.addActor(consumer1, mapOf("input" to String::class))
        builder.addActor(consumer2, mapOf("input" to String::class))
        
        // Connect producer to multiple consumers
        builder.connect(producer, "output", consumer1, "input")
        builder.connect(producer, "output", consumer2, "input")
        
        val workflowManager = builder.build()
        assertNotNull(workflowManager)
    }
    
    // ===== ERROR CASE TESTS =====
    
    @Test
    fun testConnectNonExistentActors() = runTest {
        val builder = buildActorNetworkAsync()
        
        val actor1 = TestActor("actor1")
        val actor2 = TestActor("actor2")
        val actor3 = TestActor("actor3") // Not added to builder
        
        builder.addActor(actor1, mapOf("output" to String::class))
        builder.addActor(actor2, mapOf("input" to String::class))
        
        // Try to connect to an actor not in the builder
        // This should throw an IllegalArgumentException because actor3 is not part of the builder.
        val exception = assertFailsWith<IllegalArgumentException> {
            builder.connect(actor1, "output", actor3, "input")
        }
        assertTrue(exception.message?.contains("Actor not found in builder") == true, "Expected exception message to indicate missing actor.")
    }
    
    @Test
    fun testConnectWithUndefinedPortTypes() = runTest {
        val builder = buildActorNetworkAsync()
        
        val actor1 = TestActor("actor1")
        val actor2 = TestActor("actor2")
        
        // Add actors without port definitions
        builder.addActor(actor1)
        builder.addActor(actor2)
        
        // Connect without type checking (no port definitions provided)
        builder.connect(actor1, "output", actor2, "input")
        
        val workflowManager = builder.build()
        assertNotNull(workflowManager)
    }
    
    @Test
    fun testConnectIncompatibleTypesWithDetailedError() = runTest {
        val builder = buildActorNetworkAsync()
        
        val stringProducer = TestActor("string-producer")
        val intConsumer = TestActor("int-consumer")
        
        builder.addActor(stringProducer, mapOf("output" to String::class))
        builder.addActor(intConsumer, mapOf("input" to Int::class))
        
        val exception = assertFailsWith<IllegalArgumentException> {
            builder.connect(stringProducer, "output", intConsumer, "input")
        }
        
        assertTrue(exception.message?.contains("Incompatible port types") == true)
        assertTrue(exception.message?.contains("String") == true)
        assertTrue(exception.message?.contains("Int") == true)
    }
    
    // ===== BUILDER CHAIN TESTS =====
    
    @Test
    fun testBuilderMethodChaining() = runTest {
        val actor1 = TestActor("actor1")
        val actor2 = TestActor("actor2")
        
        val workflowManager = buildActorNetworkAsync()
            .addActor(actor1, mapOf("output" to String::class))
            .addActor(actor2, mapOf("input" to String::class))
            .connect(actor1, "output", actor2, "input")
            .build()
        
        assertNotNull(workflowManager)
        assertTrue(workflowManager is WorkflowManager)
    }
    
    @Test
    fun testEmptyBuilderCreatesValidWorkflowManager() = runTest {
        val builder = buildActorNetworkAsync()
        val workflowManager = builder.build()
        
        assertNotNull(workflowManager)
        assertTrue(workflowManager is WorkflowManager)
    }
    
    // ===== DEPRECATED METHOD TESTS =====
    
    @Test
    @Suppress("DEPRECATION")
    fun testDeprecatedMethodsStillWork() {
        val actor1 = TestActor("actor1")
        val actor2 = TestActor("actor2")
        
        val builder = buildActorNetwork()
        
        builder.addActorBlocking(actor1, mapOf("output" to String::class))
        builder.addActorBlocking(actor2, mapOf("input" to String::class))
        builder.connectBlocking(actor1, "output", actor2, "input")
        
        val workflowManager = builder.build()
        assertNotNull(workflowManager)
    }
}
