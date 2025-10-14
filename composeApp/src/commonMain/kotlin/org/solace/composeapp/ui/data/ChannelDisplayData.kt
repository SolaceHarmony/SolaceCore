package org.solace.composeapp.ui.data

import kotlinx.datetime.Instant

/**
 * UI-friendly representation of channel/port data for real-time display
 */
data class ChannelDisplayData(
    val id: String,
    val name: String,
    val type: String,
    val sourceActorId: String,
    val targetActorId: String,
    val connectionState: ChannelConnectionState,
    val lastActivity: Instant,
    val metrics: ChannelMetricsData
)

/**
 * Represents the state of a channel connection
 */
sealed class ChannelConnectionState {
    object Connected : ChannelConnectionState()
    object Disconnected : ChannelConnectionState()
    object Connecting : ChannelConnectionState()
    data class Error(val message: String) : ChannelConnectionState()
}

/**
 * UI-friendly representation of channel metrics
 */
data class ChannelMetricsData(
    val messagesSent: Long = 0,
    val messagesReceived: Long = 0,
    val messagesDropped: Long = 0,
    val averageLatency: Double = 0.0,
    val throughputPerSecond: Double = 0.0,
    val errorRate: Double = 0.0,
    val lastMessageTime: Long = 0
)

/**
 * Workflow display data for visualization
 */
data class WorkflowDisplayData(
    val id: String,
    val name: String,
    val state: WorkflowState,
    val actors: List<ActorDisplayData>,
    val channels: List<ChannelDisplayData>,
    val lastUpdate: Instant
)

/**
 * Represents workflow states
 */
sealed class WorkflowState {
    object Initialized : WorkflowState()
    object Running : WorkflowState()
    object Stopped : WorkflowState()
    data class Paused(val reason: String) : WorkflowState()
    data class Error(val message: String) : WorkflowState()
}