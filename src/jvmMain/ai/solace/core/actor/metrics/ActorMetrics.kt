package ai.solace.core.actor.metrics

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration

/**
 * Metrics collection for monitoring actor performance
 */
class ActorMetrics {
    private var messagesReceived = AtomicLong(0)
    private var messagesProcessed = AtomicLong(0)
    private var errors = AtomicLong(0)
    private var processingTimes = ConcurrentLinkedQueue<Long>()
    private var lastProcessingTime: Duration? = null

    fun recordMessageReceived() = messagesReceived.incrementAndGet()
    fun recordMessageProcessed() = messagesProcessed.incrementAndGet()
    fun recordError() = errors.incrementAndGet()

    fun recordProcessingTime(duration: Duration) {
        lastProcessingTime = duration
        processingTimes.offer(duration.inWholeMilliseconds)
        if (processingTimes.size > MAX_PROCESSING_TIMES) {
            processingTimes.poll()
        }
    }

    fun getMetrics(): Map<String, Any> = buildMap {
        put("messagesReceived", messagesReceived.get())
        put("messagesProcessed", messagesProcessed.get())
        put("errors", errors.get())
        put("averageProcessingTime", processingTimes.average())
        put("lastProcessingTime", lastProcessingTime?.inWholeMilliseconds ?: 0)
    }

    fun reset() {
        messagesReceived.set(0)
        messagesProcessed.set(0)
        errors.set(0)
        processingTimes.clear()
        lastProcessingTime = null
    }

    companion object {
        private const val MAX_PROCESSING_TIMES = 1000
    }
}