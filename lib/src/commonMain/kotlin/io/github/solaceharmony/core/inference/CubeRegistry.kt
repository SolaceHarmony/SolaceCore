package io.github.solaceharmony.core.inference

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owner status of an InferenceCube.
 */
enum class Owner {
    TRANSFORMER,
    MENTORING,
    LNN_OWNED,
    FROZEN
}

/**
 * State container for a single tracked InferenceCube.
 *
 * @property cubeId The unique index of the cube.
 * @property status Current owner of the cube.
 * @property alignedVersion Version of the Transformer this cube was aligned against.
 * @property errorHistory Circular/rolling history of errors.
 */
data class CubeStatus(
    val cubeId: Int,
    var status: Owner,
    var alignedVersion: Int,
    val errorHistory: List<Float>
)

/**
 * Thread-safe registry tracking status and version alignments for all InferenceCubes.
 */
interface CubeRegistry {
    suspend fun registerCube(status: CubeStatus)
    suspend fun getStatus(cubeId: Int): CubeStatus?
    suspend fun updateOwner(cubeId: Int, owner: Owner)
    suspend fun recordError(cubeId: Int, error: Float)
    suspend fun getAllStatuses(): List<CubeStatus>
}

/**
 * Thread-safe in-memory implementation of CubeRegistry using Mutex.
 */
class InMemoryCubeRegistry : CubeRegistry {
    private val mutex = Mutex()
    private val cubes = mutableMapOf<Int, CubeStatus>()

    override suspend fun registerCube(status: CubeStatus) = mutex.withLock {
        cubes[status.cubeId] = status
    }

    override suspend fun getStatus(cubeId: Int): CubeStatus? = mutex.withLock {
        cubes[cubeId]
    }

    override suspend fun updateOwner(cubeId: Int, owner: Owner) = mutex.withLock {
        val current = cubes[cubeId]
        if (current != null) {
            cubes[cubeId] = current.copy(status = owner)
        }
    }

    override suspend fun recordError(cubeId: Int, error: Float) = mutex.withLock {
        val current = cubes[cubeId]
        if (current != null) {
            val newHistory = (current.errorHistory + error).takeLast(10)
            cubes[cubeId] = current.copy(errorHistory = newHistory)
        }
    }

    override suspend fun getAllStatuses(): List<CubeStatus> = mutex.withLock {
        cubes.values.toList()
    }
}
