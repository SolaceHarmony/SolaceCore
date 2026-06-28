package io.github.solaceharmony.core.inference

import io.github.solaceharmony.core.actor.Actor
import io.github.solaceharmony.core.actor.ActorMessage
import io.github.solaceharmony.core.kernel.channels.ports.Port
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * Data packet representing the token inputs or transformer output features of a cube.
 */
data class CubeData(
    val cubeId: Int,
    val version: Int,
    val tensor: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as CubeData
        if (cubeId != other.cubeId) return false
        if (version != other.version) return false
        if (!tensor.contentEquals(other.tensor)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = cubeId
        result = 31 * result + version
        result = 31 * result + tensor.contentHashCode()
        return result
    }
}

/**
 * Wrapper Actor around the general Transformer model.
 * Chunkifies raw sequence inputs, runs transformer forward-pass per cube, 
 * writes results to SharedMemory, and routes target outputs to the Mentoring ports.
 */
class TransformerWrapperActor(
    id: String,
    name: String = "TransformerWrapper",
    private val sharedMem: SharedMemoryManager,
    private val registry: CubeRegistry
) : Actor(id = id, name = name) {

    val lnnActors = mutableMapOf<Int, LnnModuleActor>()

    fun registerLnnActor(cubeId: Int, actor: LnnModuleActor) {
        lnnActors[cubeId] = actor
    }

    suspend fun initialize() {
        createPort(
            name = "rawFeatures",
            messageClass = ActorMessage::class,
            handler = { msg ->
                val payload = msg.payload as? CubeData ?: return@createPort
                processInference(payload)
            }
        )
        
        createOutputPort(
            name = "mentoringOutput",
            messageClass = ActorMessage::class
        )

        createOutputPort(
            name = "inferenceResult",
            messageClass = ActorMessage::class
        )
    }

    private suspend fun processInference(data: CubeData) {
        val status = registry.getStatus(data.cubeId)
        val isLnnOwned = (status?.status == Owner.LNN_OWNED)

        val outputTensor = if (isLnnOwned) {
            val lnnActor = lnnActors[data.cubeId]
            if (lnnActor != null) {
                // Takeover in action: Use LNN prediction
                lnnActor.predict(data.tensor)
            } else {
                runTransformerInference(data.cubeId, data.tensor)
            }
        } else {
            runTransformerInference(data.cubeId, data.tensor)
        }

        // Write the outcome back to simulated SharedMemory
        if (sharedMem is InMemorySharedMemoryManager) {
            sharedMem.writeSlice(data.cubeId, outputTensor)
        }

        // Only send to mentoring if not fully owned by LNN yet
        if (!isLnnOwned) {
            getPort("mentoringOutput", ActorMessage::class)?.send(
                ActorMessage(
                    correlationId = data.cubeId.toString(),
                    payload = CubeData(data.cubeId, data.version, outputTensor),
                    sender = name
                )
            )
        }

        // Expose final inference output
        getPort("inferenceResult", ActorMessage::class)?.send(
            ActorMessage(
                correlationId = data.cubeId.toString(),
                payload = CubeData(data.cubeId, data.version, outputTensor),
                sender = name
            )
        )
    }

    private fun runTransformerInference(cubeId: Int, input: FloatArray): FloatArray {
        // Simulate Transformer forward pass: multiply input values by 2.0f
        return FloatArray(input.size) { input[it] * 2.0f }
    }
}

/**
 * Mentored LTC Network Actor dedicated to learning a specific cube's temporal mappings.
 */
class LnnModuleActor(
    id: String,
    name: String,
    val cubeId: Int,
    private val registry: CubeRegistry
) : Actor(id = id, name = name) {

    private var trainingCount = 0

    suspend fun initialize() {
        createPort(
            name = "mentoringInput",
            messageClass = ActorMessage::class,
            handler = { msg ->
                val payload = msg.payload as? CubeData ?: return@createPort
                trainStep(payload.tensor)
            }
        )
    }

    private suspend fun trainStep(target: FloatArray) {
        trainingCount++
        // Simulate error decay over training iterations
        val simulatedMse = 0.02f / trainingCount
        registry.recordError(cubeId, simulatedMse)
    }

    fun predict(input: FloatArray): FloatArray {
        // Simulate LTC dynamic integration close to Transformer (e.g. factor 1.95f)
        return FloatArray(input.size) { input[it] * 1.95f }
    }
}

/**
 * Controller Actor that polls the registry and handles Progressive Takeover transitions.
 */
class GatingControllerActor(
    id: String,
    name: String = "GatingController",
    private val registry: CubeRegistry,
    private val errorThreshold: Float = 0.01f,
    private val hysteresisMargin: Float = 1.5f
) : Actor(id = id, name = name) {

    private var pollingJob: kotlinx.coroutines.Job? = null

    override suspend fun start() {
        super.start()
        pollingJob = scope.launch {
            while (isActive()) {
                pollRegistry()
                kotlinx.coroutines.delay(20) // Poll frequently for tests
            }
        }
    }

    override suspend fun stop() {
        pollingJob?.cancel()
        super.stop()
    }

    private suspend fun pollRegistry() {
        for (status in registry.getAllStatuses()) {
            if (status.status == Owner.MENTORING) {
                if (status.errorHistory.isNotEmpty() && status.errorHistory.average() < errorThreshold) {
                    registry.updateOwner(status.cubeId, Owner.LNN_OWNED)
                }
            } else if (status.status == Owner.LNN_OWNED) {
                if (status.errorHistory.isNotEmpty() && status.errorHistory.last() > errorThreshold * hysteresisMargin) {
                    registry.updateOwner(status.cubeId, Owner.MENTORING)
                }
            }
        }
    }
}

/**
 * Dream Engine Actor that handles offline consolidation / decay-prevention replays.
 */
class DreamEngineActor(
    id: String,
    name: String = "DreamEngine",
    private val registry: CubeRegistry
) : Actor(id = id, name = name) {

    suspend fun startDreamCycle(history: List<FloatArray>, lnnActor: LnnModuleActor) {
        // Replay historical slices back to LnnModuleActor
        val port = lnnActor.getPort("mentoringInput", ActorMessage::class)
        for (snapshot in history) {
            port?.send(
                ActorMessage(
                    payload = CubeData(lnnActor.cubeId, 1, snapshot),
                    sender = name
                )
            )
        }
    }
}
