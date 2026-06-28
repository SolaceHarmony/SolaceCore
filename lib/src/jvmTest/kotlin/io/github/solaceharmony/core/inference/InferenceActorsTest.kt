package io.github.solaceharmony.core.inference

import io.github.solaceharmony.core.actor.ActorMessage
import io.github.solaceharmony.core.kernel.channels.ports.Port
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InferenceActorsTest {

    @Test
    fun testTransformerToLnnTakeoverFlow() = runBlocking {
        val sharedMem = InMemorySharedMemoryManager()
        sharedMem.allocate(1, 4)
        
        val registry = InMemoryCubeRegistry()
        registry.registerCube(CubeStatus(0, Owner.MENTORING, 1, emptyList()))

        val transformerActor = TransformerWrapperActor("tx-wrapper", "TransformerWrapper", sharedMem, registry)
        val lnnActor = LnnModuleActor("lnn-0", "LnnModule-0", 0, registry)
        val gatingController = GatingControllerActor("gating", "GatingController", registry, errorThreshold = 0.01f)

        transformerActor.registerLnnActor(0, lnnActor)

        transformerActor.initialize()
        lnnActor.initialize()

        // Wire mentoring: TransformerWrapper.mentoringOutput -> LnnModule.mentoringInput
        val txOutputPort = transformerActor.getPort("mentoringOutput", ActorMessage::class)!!
        val lnnInputPort = lnnActor.getPort("mentoringInput", ActorMessage::class)!!
        val mentoringConnection = Port.connect(txOutputPort, lnnInputPort)
        mentoringConnection.start(this)

        // Capture inference results by connecting resultsPort to a dummy receiver
        val resultsPort = transformerActor.getPort("inferenceResult", ActorMessage::class)!!
        val dummyPort = io.github.solaceharmony.core.kernel.channels.ports.BidirectionalPort(
            name = "dummy",
            type = ActorMessage::class
        )
        val resultsConnection = Port.connect(resultsPort, dummyPort)
        resultsConnection.start(this)

        transformerActor.start()
        lnnActor.start()
        gatingController.start()

        val txInputPort = transformerActor.getPort("rawFeatures", ActorMessage::class)!!
        val inputData = FloatArray(4) { 1.0f }

        // --- STEP 1: First Inference ---
        println("Test: Sending first raw features...")
        txInputPort.send(ActorMessage(payload = CubeData(0, 1, inputData)))
        
        // Wait and check result (Should be Transformer output: 2.0f * input = 2.0f)
        val msg1 = dummyPort.asChannel().receive()
        val out1 = (msg1.payload as CubeData).tensor
        assertEquals(2.0f, out1[0], "First output should come from Transformer (2.0f * input)")

        // Wait for registry error update
        withTimeout(2000) {
            while ((registry.getStatus(0)?.errorHistory?.size ?: 0) < 1) {
                delay(10)
            }
        }
        assertEquals(0.02f, registry.getStatus(0)!!.errorHistory.last())
        assertEquals(Owner.MENTORING, registry.getStatus(0)!!.status)

        // --- STEP 2: Second Inference ---
        println("Test: Sending second raw features...")
        txInputPort.send(ActorMessage(payload = CubeData(0, 1, inputData)))
        dummyPort.asChannel().receive()
        // Wait for second error update
        withTimeout(2000) {
            while ((registry.getStatus(0)?.errorHistory?.size ?: 0) < 2) {
                delay(10)
            }
        }
        assertEquals(0.01f, registry.getStatus(0)!!.errorHistory.last())
        assertEquals(Owner.MENTORING, registry.getStatus(0)!!.status) // Average is (0.02 + 0.01)/2 = 0.015 > 0.01, still Mentoring

        // --- STEP 3: Multiple inferences to drop average below threshold ---
        println("Test: Sending third raw features...")
        txInputPort.send(ActorMessage(payload = CubeData(0, 1, inputData)))
        dummyPort.asChannel().receive()
        withTimeout(2000) {
            while ((registry.getStatus(0)?.errorHistory?.size ?: 0) < 3) {
                delay(10)
            }
        }

        println("Test: Sending fourth raw features...")
        txInputPort.send(ActorMessage(payload = CubeData(0, 1, inputData)))
        dummyPort.asChannel().receive()
        withTimeout(2000) {
            while ((registry.getStatus(0)?.errorHistory?.size ?: 0) < 4) {
                delay(10)
            }
        }

        println("Test: Sending fifth raw features...")
        txInputPort.send(ActorMessage(payload = CubeData(0, 1, inputData)))
        dummyPort.asChannel().receive()
        withTimeout(2000) {
            while ((registry.getStatus(0)?.errorHistory?.size ?: 0) < 5) {
                delay(10)
            }
        }

        // Wait for GatingController to update owner status to LNN_OWNED
        withTimeout(2000) {
            while (registry.getStatus(0)?.status != Owner.LNN_OWNED) {
                delay(10)
            }
        }
        assertEquals(Owner.LNN_OWNED, registry.getStatus(0)!!.status, "GatingController should promote cube to LNN_OWNED")

        // --- STEP 4: Sixth Inference (Takeover Active) ---
        println("Test: Sending sixth raw features (Post-Takeover)...")
        txInputPort.send(ActorMessage(payload = CubeData(0, 1, inputData)))
        val msg6 = dummyPort.asChannel().receive()
        val out6 = (msg6.payload as CubeData).tensor
        // Bypassed Transformer! Output must come from LNN: 1.95f * input = 1.95f
        assertEquals(1.95f, out6[0], "Output should come from LNN (1.95f * input)")

        // Cleanup
        mentoringConnection.stop()
        resultsConnection.stop()
        dummyPort.dispose()
        transformerActor.dispose()
        lnnActor.dispose()
        gatingController.dispose()
    }
}
