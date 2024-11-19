package ai.solace.core.common

/**
 * Platform-independent interface for lifecycle management
 */
interface Lifecycle : Disposable {
    suspend fun start()
    suspend fun stop()
    fun isActive(): Boolean
}