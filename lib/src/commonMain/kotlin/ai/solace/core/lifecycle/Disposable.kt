package ai.solace.core.lifecycle

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
    suspend fun safeDispose() {
        try {
            dispose()
        } catch (e: Exception) {
            println("Error during disposal: ${e.message}")
        }
    }
}