package ai.solace.core.common

/**
 * Platform-independent interface for resource management
 */
interface Disposable {
    suspend fun dispose()

    companion object {
        suspend fun dispose(vararg disposables: Disposable) {
            disposables.forEach { it.dispose() }
        }
    }
}