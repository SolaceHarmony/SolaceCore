package ai.solace.core.actor.metrics

import kotlinx.atomicfu.atomic
import kotlin.time.Duration

/**
 * Metrics collection for monitoring actor performance
 */
class ActorMetrics {
    private var messagesReceived = atomic(0L)
    private var messagesProcessed = atomic(0L)
    private var errors = atomic(0L)
    private val processingTimes = mutableListOf<Long>()
    private var lastProcessingTime: Duration? = null

    fun recordMessageReceived() = messagesReceived.incrementAndGet()
    fun recordMessageProcessed() = messagesProcessed.incrementAndGet()
    fun recordError() = errors.incrementAndGet()

    fun recordProcessingTime(duration: Duration) {
        lastProcessingTime = duration
        synchronized(processingTimes) {
            processingTimes.add(duration.inWholeMilliseconds)
            if (processingTimes.size > MAX_PROCESSING_TIMES) {
                processingTimes.removeFirst()
            }
        }
    }

    fun getMetrics(): Map<String, Any> = buildMap {
        put("messagesReceived", messagesReceived.value)
        put("messagesProcessed", messagesProcessed.value)
        put("errors", errors.value)
        synchronized(processingTimes) {
            put("averageProcessingTime", processingTimes.average())
        }
        put("lastProcessingTime", lastProcessingTime?.inWholeMilliseconds ?: 0)
    }

    fun reset() {
        messagesReceived.value = 0
        messagesProcessed.value = 0
        errors.value = 0
        synchronized(processingTimes) {
            processingTimes.clear()
        }
        lastProcessingTime = null
    }

    companion object {
        private const val MAX_PROCESSING_TIMES = 1000
    }
}