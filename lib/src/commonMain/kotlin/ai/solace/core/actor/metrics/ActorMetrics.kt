package ai.solace.core.actor.metrics

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.AtomicLong
import kotlin.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Comprehensive metrics collection for actor performance monitoring
 */
class ActorMetrics {
    // Message counters
    private val messagesReceived: AtomicLong = atomic(0L)
    private val messagesProcessed: AtomicLong = atomic(0L)
    private val messagesFailed: AtomicLong = atomic(0L)

    // Protocol-specific counters
    private val protocolMessagesReceived = ConcurrentHashMap<String, AtomicLong>()
    private val protocolMessagesProcessed = ConcurrentHashMap<String, AtomicLong>()
    private val protocolMessagesFailed = ConcurrentHashMap<String, AtomicLong>()

    // Processing time tracking
    private val processingTimes = ArrayList<Long>()
    private var lastProcessingTime: Duration? = null
    private val processingTimeMutex = Mutex()

    // Priority-based metrics
    private val priorityMessageCounts = ConcurrentHashMap<String, AtomicLong>()

    // Port metrics
    private val portMessageCounts = ConcurrentHashMap<String, AtomicLong>()

    /**
     * Records a received message
     * @param protocol Optional protocol identifier for protocol-specific metrics
     */
    fun recordMessageReceived(protocol: String? = null) {
        messagesReceived.incrementAndGet()
        protocol?.let {
            protocolMessagesReceived.computeIfAbsent(it) { atomic(0L) }.incrementAndGet()
        }
    }

    /**
     * Records a processed message
     * @param protocol Optional protocol identifier for protocol-specific metrics
     */
    fun recordMessageProcessed(protocol: String? = null) {
        messagesProcessed.incrementAndGet()
        protocol?.let {
            protocolMessagesProcessed.computeIfAbsent(it) { atomic(0L) }.incrementAndGet()
        }
    }

    /**
     * Records an error in message processing
     * @param protocol Optional protocol identifier for protocol-specific metrics
     */
    fun recordError(protocol: String? = null) {
        messagesFailed.incrementAndGet()
        protocol?.let {
            protocolMessagesFailed.computeIfAbsent(it) { atomic(0L) }.incrementAndGet()
        }
    }

    /**
     * Records message processing time
     * @param duration The time taken to process the message
     * @param port Optional port identifier for port-specific metrics
     */
    suspend fun recordProcessingTime(duration: Duration, port: String? = null) {
        lastProcessingTime = duration
        processingTimeMutex.withLock {
            processingTimes.add(duration.inWholeMilliseconds)
            if (processingTimes.size > MAX_PROCESSING_TIMES) {
                processingTimes.removeFirst()
            }
        }
        port?.let {
            portMessageCounts.computeIfAbsent(it) { atomic(0L) }.incrementAndGet()
        }
    }

    /**
     * Records a message with specific priority
     * @param priority The priority level of the message
     */
    fun recordPriorityMessage(priority: String) {
        priorityMessageCounts.computeIfAbsent(priority) { atomic(0L) }.incrementAndGet()
    }

    /**
     * Retrieves all current metrics
     * @return Map of metric names to values
     */
    suspend fun getMetrics(): Map<String, Any> = buildMap {
        // Basic metrics
        put("messagesReceived", messagesReceived.value)
        put("messagesProcessed", messagesProcessed.value)
        put("messagesFailed", messagesFailed.value)

        // Processing time metrics
        processingTimeMutex.withLock {
            put("averageProcessingTime", processingTimes.average())
            put("maxProcessingTime", processingTimes.maxOrNull() ?: 0)
            put("minProcessingTime", processingTimes.minOrNull() ?: 0)
        }
        put("lastProcessingTime", lastProcessingTime?.inWholeMilliseconds ?: 0)

        // Protocol metrics
        put("protocolMetrics", buildMap {
            protocolMessagesReceived.forEach { (protocol, count) ->
                put("$protocol.received", count.value)
                put("$protocol.processed", protocolMessagesProcessed[protocol]?.value ?: 0)
                put("$protocol.failed", protocolMessagesFailed[protocol]?.value ?: 0)
            }
        })

        // Priority metrics
        put("priorityMetrics", buildMap {
            priorityMessageCounts.forEach { (priority, count) ->
                put(priority, count.value)
            }
        })

        // Port metrics
        put("portMetrics", buildMap {
            portMessageCounts.forEach { (port, count) ->
                put(port, count.value)
            }
        })

        // Calculate success rate
        val totalMessages = messagesProcessed.value + messagesFailed.value
        if (totalMessages > 0) {
            val successRate = (messagesProcessed.value.toDouble() / totalMessages) * 100
            put("successRate", successRate)
        }
    }

    /**
     * Resets all metrics to their initial values
     */
    suspend fun reset() {
        messagesReceived.value = 0
        messagesProcessed.value = 0
        messagesFailed.value = 0

        protocolMessagesReceived.clear()
        protocolMessagesProcessed.clear()
        protocolMessagesFailed.clear()

        processingTimeMutex.withLock {
            processingTimes.clear()
        }
        lastProcessingTime = null

        priorityMessageCounts.clear()
        portMessageCounts.clear()
    }

    companion object {
        private const val MAX_PROCESSING_TIMES = 1000
    }
}