package io.github.solaceharmony.core.inference

/**
 * Manages zero-copy shared memory blocks for InferenceCube token slice processing.
 */
interface SharedMemoryManager {
    /**
     * Allocates shared memory buffer for a set of cubes and token block sizes.
     *
     * @param numCubes Number of independent cubes to allocate.
     * @param blockSize Size of each slice block in floats.
     */
    fun allocate(numCubes: Int, blockSize: Int)

    /**
     * Retrieves a zero-copy FloatArray view slice pointing to the shared buffer for the given cubeId.
     *
     * @param cubeId The identifier of the target cube slice.
     * @return The float array slice corresponding to the cube.
     */
    fun getSlice(cubeId: Int): FloatArray
}

/**
 * In-memory simulator of SharedMemoryManager.
 * Simulates block-level reads/writes to a contiguous float buffer.
 */
class InMemorySharedMemoryManager : SharedMemoryManager {
    private var buffer: FloatArray = FloatArray(0)
    private var blockSize: Int = 0

    override fun allocate(numCubes: Int, blockSize: Int) {
        this.blockSize = blockSize
        this.buffer = FloatArray(numCubes * blockSize)
    }

    override fun getSlice(cubeId: Int): FloatArray {
        if (blockSize == 0) return FloatArray(0)
        val start = cubeId * blockSize
        val end = start + blockSize
        if (start < 0 || end > buffer.size) return FloatArray(blockSize)
        return buffer.copyOfRange(start, end)
    }

    /**
     * Helper to write back data into the shared buffer.
     */
    fun writeSlice(cubeId: Int, data: FloatArray) {
        val start = cubeId * blockSize
        if (start >= 0 && start + data.size <= buffer.size) {
            data.copyInto(buffer, start)
        }
    }
}
