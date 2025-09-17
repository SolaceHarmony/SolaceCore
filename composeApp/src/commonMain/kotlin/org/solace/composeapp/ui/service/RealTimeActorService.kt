@file:OptIn(ExperimentalUuidApi::class)
package org.solace.composeapp.ui.service

import org.solace.composeapp.actor.ActorState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import org.solace.composeapp.ui.data.ActorDisplayData
import org.solace.composeapp.ui.data.ActorMetricsData
import org.solace.composeapp.ui.data.SystemMetricsData
import org.solace.composeapp.ui.data.ChannelDisplayData
import org.solace.composeapp.ui.data.ChannelConnectionState
import org.solace.composeapp.ui.data.ChannelMetricsData
import org.solace.composeapp.ui.data.WorkflowDisplayData
import org.solace.composeapp.ui.data.WorkflowState
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Service that provides real-time updates for the actor system UI
 */
class RealTimeActorService(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _actors = MutableStateFlow<List<ActorDisplayData>>(emptyList())
    val actors: StateFlow<List<ActorDisplayData>> = _actors.asStateFlow()
    
    private val _channels = MutableStateFlow<List<ChannelDisplayData>>(emptyList())
    val channels: StateFlow<List<ChannelDisplayData>> = _channels.asStateFlow()
    
    private val _workflows = MutableStateFlow<List<WorkflowDisplayData>>(emptyList())
    val workflows: StateFlow<List<WorkflowDisplayData>> = _workflows.asStateFlow()
    
    private val _selectedWorkflow = MutableStateFlow<WorkflowDisplayData?>(null)
    val selectedWorkflow: StateFlow<WorkflowDisplayData?> = _selectedWorkflow.asStateFlow()
    
    private val _systemMetrics = MutableStateFlow(SystemMetricsData())
    val systemMetrics: StateFlow<SystemMetricsData> = _systemMetrics.asStateFlow()
    
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    
    private var monitoringJob: Job? = null
    
    /**
     * Start real-time monitoring of the actor system
     */
    fun startMonitoring() {
        if (_isMonitoring.value) return
        
        _isMonitoring.value = true
        monitoringJob = scope.launch {
            while (_isMonitoring.value) {
                updateActorData()
                updateChannelData()
                updateWorkflowData()
                updateSystemMetrics()
                delay(UPDATE_INTERVAL_MS)
            }
        }
    }
    
    /**
     * Stop real-time monitoring
     */
    fun stopMonitoring() {
        _isMonitoring.value = false
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    /**
     * Simulate actor data for demonstration purposes
     */
    private suspend fun updateActorData() {
        val currentTime = Clock.System.now()
        
        // For now, create sample data until we have actual supervisor integration
        val sampleActors = listOf(
            ActorDisplayData(
                id = DEMO_ACTOR_MESSAGE_PROCESSOR,
                name = "Message Processor",
                state = ActorState.Running,
                lastUpdate = currentTime,
                metrics = ActorMetricsData(
                    messagesReceived = (500..1500).random().toLong(),
                    messagesProcessed = (400..1200).random().toLong(),
                    messagesFailed = (0..50).random().toLong(),
                    successRate = (85..99).random().toDouble(),
                    averageProcessingTime = (10..100).random().toDouble(),
                    lastProcessingTime = (5..50).random().toLong()
                )
            ),
            ActorDisplayData(
                id = DEMO_ACTOR_DATA_TRANSFORMER,
                name = "Data Transformer",
                state = if ((0..10).random() > 7) ActorState.Paused("Maintenance") else ActorState.Running,
                lastUpdate = currentTime,
                metrics = ActorMetricsData(
                    messagesReceived = (200..800).random().toLong(),
                    messagesProcessed = (180..750).random().toLong(),
                    messagesFailed = (0..20).random().toLong(),
                    successRate = (90..100).random().toDouble(),
                    averageProcessingTime = (15..80).random().toDouble(),
                    lastProcessingTime = (8..30).random().toLong()
                )
            ),
            ActorDisplayData(
                id = DEMO_ACTOR_RESULT_AGGREGATOR,
                name = "Result Aggregator",
                state = if ((0..10).random() > 8) ActorState.Error("Connection timeout") else ActorState.Running,
                lastUpdate = currentTime,
                metrics = ActorMetricsData(
                    messagesReceived = (100..600).random().toLong(),
                    messagesProcessed = (80..500).random().toLong(),
                    messagesFailed = (5..80).random().toLong(),
                    successRate = (70..95).random().toDouble(),
                    averageProcessingTime = (25..150).random().toDouble(),
                    lastProcessingTime = (50..200).random().toLong()
                )
            ),
            ActorDisplayData(
                id = DEMO_ACTOR_LOGGER_SERVICE,
                name = "Logger Service",
                state = ActorState.Running,
                lastUpdate = currentTime,
                metrics = ActorMetricsData(
                    messagesReceived = (1000..2000).random().toLong(),
                    messagesProcessed = (950..1950).random().toLong(),
                    messagesFailed = (0..10).random().toLong(),
                    successRate = (98..100).random().toDouble(),
                    averageProcessingTime = (5..20).random().toDouble(),
                    lastProcessingTime = (2..10).random().toLong()
                )
            )
        )
        
        _actors.value = sampleActors
    }
    
    /**
     * Simulate channel data for demonstration
     */
    private suspend fun updateChannelData() {
        val currentTime = Clock.System.now()
        val currentActors = _actors.value
        
        val sampleChannels = listOf(
            ChannelDisplayData(
                id = DEMO_CHANNEL_INPUT,
                name = "Input Channel",
                type = "String",
                sourceActorId = "External",
                targetActorId = DEMO_ACTOR_MESSAGE_PROCESSOR,
                connectionState = ChannelConnectionState.Connected,
                lastActivity = currentTime,
                metrics = ChannelMetricsData(
                    messagesSent = (100..500).random().toLong(),
                    messagesReceived = (95..495).random().toLong(),
                    messagesDropped = (0..5).random().toLong(),
                    averageLatency = (1..10).random().toDouble(),
                    throughputPerSecond = (10..50).random().toDouble(),
                    errorRate = (0..2).random().toDouble()
                )
            ),
            ChannelDisplayData(
                id = DEMO_CHANNEL_PROCESSING,
                name = "Processing Pipeline",
                type = "ProcessedMessage",
                sourceActorId = DEMO_ACTOR_MESSAGE_PROCESSOR,
                targetActorId = DEMO_ACTOR_DATA_TRANSFORMER,
                connectionState = if ((0..10).random() > 8) ChannelConnectionState.Error("Network timeout") 
                                else ChannelConnectionState.Connected,
                lastActivity = currentTime,
                metrics = ChannelMetricsData(
                    messagesSent = (80..400).random().toLong(),
                    messagesReceived = (75..395).random().toLong(),
                    messagesDropped = (0..10).random().toLong(),
                    averageLatency = (2..15).random().toDouble(),
                    throughputPerSecond = (8..40).random().toDouble(),
                    errorRate = (0..5).random().toDouble()
                )
            ),
            ChannelDisplayData(
                id = DEMO_CHANNEL_AGGREGATION,
                name = "Aggregation Channel",
                type = "TransformedData",
                sourceActorId = DEMO_ACTOR_DATA_TRANSFORMER,
                targetActorId = DEMO_ACTOR_RESULT_AGGREGATOR,
                connectionState = ChannelConnectionState.Connected,
                lastActivity = currentTime,
                metrics = ChannelMetricsData(
                    messagesSent = (60..300).random().toLong(),
                    messagesReceived = (58..295).random().toLong(),
                    messagesDropped = (0..5).random().toLong(),
                    averageLatency = (5..25).random().toDouble(),
                    throughputPerSecond = (5..30).random().toDouble(),
                    errorRate = (0..3).random().toDouble()
                )
            ),
            ChannelDisplayData(
                id = DEMO_CHANNEL_LOGGING,
                name = "Logging Channel",
                type = "LogMessage",
                sourceActorId = DEMO_ACTOR_RESULT_AGGREGATOR,
                targetActorId = DEMO_ACTOR_LOGGER_SERVICE,
                connectionState = ChannelConnectionState.Connected,
                lastActivity = currentTime,
                metrics = ChannelMetricsData(
                    messagesSent = (50..250).random().toLong(),
                    messagesReceived = (50..250).random().toLong(),
                    messagesDropped = (0..2).random().toLong(),
                    averageLatency = (1..5).random().toDouble(),
                    throughputPerSecond = (15..60).random().toDouble(),
                    errorRate = (0..1).random().toDouble()
                )
            )
        )
        
        _channels.value = sampleChannels
    }
    
    /**
     * Simulate workflow data
     */
    private suspend fun updateWorkflowData() {
        val currentTime = Clock.System.now()
        val currentActors = _actors.value
        val currentChannels = _channels.value
        
        val sampleWorkflow = WorkflowDisplayData(
            id = "workflow-1",
            name = "Message Processing Pipeline",
            state = WorkflowState.Running,
            actors = currentActors,
            channels = currentChannels,
            lastUpdate = currentTime
        )
        
        _workflows.value = listOf(sampleWorkflow)
        if (_selectedWorkflow.value == null) {
            _selectedWorkflow.value = sampleWorkflow
        }
    }
    
    /**
     * Update system-wide metrics
     */
    private suspend fun updateSystemMetrics() {
        val currentActors = _actors.value
        val runningCount = currentActors.count { it.state is ActorState.Running }
        val pausedCount = currentActors.count { it.state is ActorState.Paused }
        val errorCount = currentActors.count { it.state is ActorState.Error }
        val stoppedCount = currentActors.count { it.state is ActorState.Stopped }
        
        val totalMessages = currentActors.sumOf { it.metrics.messagesReceived }
        val avgResponseTime = if (currentActors.isNotEmpty()) {
            currentActors.map { it.metrics.averageProcessingTime }.average()
        } else 0.0
        
        _systemMetrics.value = SystemMetricsData(
            totalActors = currentActors.size,
            runningActors = runningCount,
            stoppedActors = stoppedCount,
            errorActors = errorCount,
            pausedActors = pausedCount,
            totalMessages = totalMessages,
            averageResponseTime = avgResponseTime,
            systemUptime = Clock.System.now().toEpochMilliseconds()
        )
    }
    
    /**
     * Actor lifecycle management methods
     */
    fun createActor() {
        // In a real implementation, this would interface with the actor system
        println("Creating new actor...")
    }
    
    fun deleteActor(actorId: String) {
        println("Deleting actor: $actorId")
    }
    
    fun startActor(actorId: String) {
        println("Starting actor: $actorId")
    }
    
    fun stopActor(actorId: String) {
        println("Stopping actor: $actorId")
    }
    
    fun pauseActor(actorId: String) {
        println("Pausing actor: $actorId")
    }
    
    fun resumeActor(actorId: String) {
        println("Resuming actor: $actorId")
    }
    
    companion object {
        private const val UPDATE_INTERVAL_MS = 2000L // 2 second updates for demo
        
        // Stable UUID-based IDs for demo actors to ensure consistency across UI updates
        // Using predictable UUIDs for demo purposes while maintaining uniqueness guarantees
        private val DEMO_ACTOR_MESSAGE_PROCESSOR = "a1234567-1234-4123-8123-123456789012"
        private val DEMO_ACTOR_DATA_TRANSFORMER = "a2345678-2345-4234-8234-234567890123"  
        private val DEMO_ACTOR_RESULT_AGGREGATOR = "a3456789-3456-4345-8345-345678901234"
        private val DEMO_ACTOR_LOGGER_SERVICE = "a4567890-4567-4456-8456-456789012345"
        
        // Stable UUID-based IDs for demo channels
        private val DEMO_CHANNEL_INPUT = "c1234567-1234-4123-8123-123456789012"
        private val DEMO_CHANNEL_PROCESSING = "c2345678-2345-4234-8234-234567890123"
        private val DEMO_CHANNEL_AGGREGATION = "c3456789-3456-4345-8345-345678901234" 
        private val DEMO_CHANNEL_LOGGING = "c4567890-4567-4456-8456-456789012345"
    }
}