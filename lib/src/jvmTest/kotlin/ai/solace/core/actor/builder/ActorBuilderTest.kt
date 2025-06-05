package ai.solace.core.actor.builder

import ai.solace.core.actor.Actor
import ai.solace.core.workflow.WorkflowManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.reflect.KClass

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
        val workflowManager = builder.build()

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
        val workflowManager = builder.build()

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
        val workflowManager = builder.build()

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
        val workflowManager = builder.build()

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
}
