package org.solace.composeapp.ui.data

import org.solace.composeapp.actor.ActorState
import kotlinx.datetime.Instant

/**
 * UI-friendly representation of actor data for real-time display
 */
data class ActorDisplayData(
    val id: String,
    val name: String,
    val state: ActorState,
    val lastUpdate: Instant,
    val metrics: ActorMetricsData
)

/**
 * UI-friendly representation of actor metrics
 */
data class ActorMetricsData(
    val messagesReceived: Long = 0,
    val messagesProcessed: Long = 0,
    val messagesFailed: Long = 0,
    val successRate: Double = 0.0,
    val averageProcessingTime: Double = 0.0,
    val lastProcessingTime: Long = 0,
    val portMetrics: Map<String, Long> = emptyMap(),
    val priorityMetrics: Map<String, Long> = emptyMap()
)

/**
 * System-wide metrics for the actor system
 */
data class SystemMetricsData(
    val totalActors: Int = 0,
    val runningActors: Int = 0,
    val stoppedActors: Int = 0,
    val errorActors: Int = 0,
    val pausedActors: Int = 0,
    val totalMessages: Long = 0,
    val averageResponseTime: Double = 0.0,
    val systemUptime: Long = 0
)