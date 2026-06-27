<!-- topic: Runtime -->
<!-- title: Actor Metrics -->

### 4.3. Actor Metrics (`metrics` Subdirectory)
The `io.github.solaceharmony.core.actor.metrics` package provides a dedicated class for collecting and managing performance and operational metrics for individual actors.

#### 4.3.1. `ActorMetrics` Class
This class offers a comprehensive suite of metrics to monitor an actor's behavior and performance. Instances of this class are typically held by each `Actor` (as seen in the `Actor` base class).

*   **Purpose:** To track key performance indicators (KPIs) such as message throughput, processing times, error rates, and distribution of messages by protocol, priority, or port.
*   **Key Metrics Collected:**
    *   **General Message Statistics:**
        *   `messagesReceived`: Total count of messages received by the actor.
        *   `messagesProcessed`: Total count of messages successfully processed.
        *   `messagesFailed`: Total count of messages that resulted in processing errors.
        *   (Uses `kotlinx.atomicfu.AtomicLong` for thread-safe counting).
    *   **Protocol-Specific Statistics:** (Stored in `ConcurrentHashMap<String, AtomicLong>`)
        *   Counts of received, processed, and failed messages, broken down by a `protocol` string identifier.
    *   **Processing Time Statistics:**
        *   `lastProcessingTime`: `Duration` of the most recent message processing.
        *   A rolling list (`ArrayList<Long>`) of the last `MAX_PROCESSING_TIMES` (default 1000) processing durations (in milliseconds) is kept to calculate:
            *   `averageProcessingTime`
            *   `maxProcessingTime`
            *   `minProcessingTime`
        *   Access to the `processingTimes` list is synchronized using a `Mutex`.
    *   **Priority-Based Statistics:** (Stored in `ConcurrentHashMap<String, AtomicLong>`)
        *   Counts of messages processed, categorized by their priority level (e.g., "HIGH", "NORMAL", "LOW").
    *   **Port-Specific Statistics:** (Stored in `ConcurrentHashMap<String, AtomicLong>`)
        *   Counts of messages processed through specific actor ports, identified by port name.
*   **Metric Recording Methods:**
    *   `fun recordMessageReceived(protocol: String? = null)`
    *   `fun recordMessageProcessed(protocol: String? = null)`
    *   `fun recordError(protocol: String? = null)`
    *   `suspend fun recordProcessingTime(duration: Duration, port: String? = null)`
    *   `fun recordPriorityMessage(priority: String)`
*   **Metric Retrieval:**
    *   `suspend fun getMetrics(): Map<String, Any>`: Asynchronously compiles and returns a map containing all current metric values. This map includes calculated metrics like `successRate` and organizes protocol, priority, and port metrics into nested maps for clarity.
*   **Reset Functionality:**
    *   `suspend fun reset()`: Resets all collected metrics to their initial zeroed/empty state.

The `ActorMetrics` class is essential for observing the runtime behavior of actors, identifying bottlenecks, and ensuring the overall health of the actor system.


[Back to Actor System](Actor-System)
