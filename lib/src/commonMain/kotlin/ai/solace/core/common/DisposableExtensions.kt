package ai.solace.core.common

suspend fun Disposable.safeDispose() {
    try {
        dispose()
    } catch (e: Exception) {
        println("Error during disposal: ${e.message}")
    }
}