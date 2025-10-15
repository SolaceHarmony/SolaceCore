@file:OptIn(ExperimentalUuidApi::class)
package org.solace.composeapp.ui.service

import org.solace.composeapp.actor.ActorState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlin.math.roundToInt
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
    // Preserve user-created/deleted actors and state between ticks
    private val actorStore: MutableMap<String, ActorDisplayData> = mutableMapOf()
    
    /**
     * Start real-time monitoring of the actor system
     */
    fun startMonitoring() {
        if (_isMonitoring.value) return
        
        _isMonitoring.value = true
        // Seed once, then keep state persistent
        if (actorStore.isEmpty()) seedDemoActors()
        _actors.value = actorStore.values.toList()
        monitoringJob = scope.launch {
            while (_isMonitoring.value) {
                updateActorMetrics()
                updateChannelsFromActors()
                updateWorkflowFromState()
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

    /** Update only metrics for existing actors; keep user edits persistent. */
    private suspend fun updateActorMetrics() {
        val now = Clock.System.now()
        if (actorStore.isEmpty() && _actors.value.isNotEmpty()) {
            _actors.value.forEach { actorStore[it.id] = it }
        }
        actorStore.replaceAll { _, a ->
            val received = (200..1500).random().toLong()
            val processed = (received * (70..100).random() / 100.0).toLong()
            val failed = (0..(received/10).toInt()).random().toLong()
            val success = if (received > 0) (100.0 * (processed - failed).coerceAtLeast(0) / received).coerceIn(0.0, 100.0) else a.metrics.successRate
            a.copy(
                lastUpdate = now,
                metrics = a.metrics.copy(
                    messagesReceived = received,
                    messagesProcessed = processed,
                    messagesFailed = failed,
                    successRate = success,
                    averageProcessingTime = (10..120).random().toDouble(),
                    lastProcessingTime = (5..60).random().toLong()
                )
            )
        }
        _actors.value = actorStore.values.toList()
    }

    /** Build demo channels from current actors instead of fixed IDs. */
    private suspend fun updateChannelsFromActors() {
        val now = Clock.System.now()
        val list = _actors.value
        if (list.size < 2) { _channels.value = emptyList(); return }
        val chans = mutableListOf<ChannelDisplayData>()
        for (i in 0 until list.size - 1) {
            val src = list[i]; val dst = list[i+1]
            val id = "chan-${src.id.take(6)}-${dst.id.take(6)}"
            val state = when ((0..100).random()) {
                in 0..5 -> ChannelConnectionState.Error("Simulated error")
                in 6..20 -> ChannelConnectionState.Connecting
                in 21..25 -> ChannelConnectionState.Disconnected
                else -> ChannelConnectionState.Connected
            }
            chans += ChannelDisplayData(
                id = id,
                name = "${src.name} → ${dst.name}",
                type = "Message",
                sourceActorId = src.id,
                targetActorId = dst.id,
                connectionState = state,
                lastActivity = now,
                metrics = ChannelMetricsData(
                    messagesSent = (50..600).random().toLong(),
                    messagesReceived = (40..590).random().toLong(),
                    messagesDropped = (0..10).random().toLong(),
                    averageLatency = (2..25).random().toDouble(),
                    throughputPerSecond = (5..60).random().toDouble(),
                    errorRate = (0..5).random().toDouble()
                )
            )
        }
        _channels.value = chans
    }

    /** Keep workflow selection in sync with current actors/channels. */
    private suspend fun updateWorkflowFromState() {
        val currentTime = Clock.System.now()
        val currentActors = _actors.value
        val currentChannels = _channels.value
        val wf = WorkflowDisplayData(
            id = "workflow-1",
            name = "Message Processing Pipeline",
            state = WorkflowState.Running,
            actors = currentActors,
            channels = currentChannels,
            lastUpdate = currentTime
        )
        _workflows.value = listOf(wf)
        if (_selectedWorkflow.value == null) _selectedWorkflow.value = wf
    }

    private fun seedDemoActors() {
        val now = Clock.System.now()
        listOf(
            ActorDisplayData(
                id = DEMO_ACTOR_MESSAGE_PROCESSOR,
                name = "Message Processor",
                state = ActorState.Running,
                lastUpdate = now,
                metrics = ActorMetricsData(0,0,0,95.0,20.0,10)
            ),
            ActorDisplayData(
                id = DEMO_ACTOR_DATA_TRANSFORMER,
                name = "Data Transformer",
                state = ActorState.Running,
                lastUpdate = now,
                metrics = ActorMetricsData(0,0,0,97.0,25.0,12)
            ),
            ActorDisplayData(
                id = DEMO_ACTOR_RESULT_AGGREGATOR,
                name = "Result Aggregator",
                state = ActorState.Running,
                lastUpdate = now,
                metrics = ActorMetricsData(0,0,0,92.0,35.0,18)
            ),
            ActorDisplayData(
                id = DEMO_ACTOR_LOGGER_SERVICE,
                name = "Logger Service",
                state = ActorState.Running,
                lastUpdate = now,
                metrics = ActorMetricsData(0,0,0,99.0,8.0,4)
            )
        ).forEach { actorStore[it.id] = it }
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
     * Actor lifecycle management mock methods for the demo UI.
     * TODO: Wire to real SolaceCore actor system (SupervisorActor/Actor APIs).
     */
    
    /**
     * Creates a new actor with mock data and adds it to the StateFlow
     * TODO: Connect to SupervisorActor.createActor() when implementing real integration
     */
    fun createActor() {
        val currentTime = Clock.System.now()
        val newActorId = "actor-${(1000..9999).random()}"
        val newActor = ActorDisplayData(
            id = newActorId,
            name = "New Actor $newActorId",
            state = ActorState.Initialized,
            lastUpdate = currentTime,
            metrics = ActorMetricsData(
                messagesReceived = 0,
                messagesProcessed = 0,
                messagesFailed = 0,
                successRate = 0.0,
                averageProcessingTime = 0.0,
                lastProcessingTime = 0
            )
        )
        actorStore[newActorId] = newActor
        _actors.value = actorStore.values.toList()
        println("Created new actor: $newActorId")
    }
    
    /**
     * Deletes an actor by removing it from the StateFlow
     * TODO: Connect to SupervisorActor.unregisterActor() when implementing real integration
     */
    fun deleteActor(actorId: String) {
        val currentActors = _actors.value
        val actorExists = currentActors.any { it.id == actorId }
        
        if (actorExists) {
            actorStore.remove(actorId)
            _actors.value = actorStore.values.toList()
            println("Deleted actor: $actorId")
        } else {
            println("Actor not found for deletion: $actorId")
        }
    }
    
    /**
     * Starts an actor by updating its state to Running in the StateFlow
     * TODO: Connect to Actor.start() when implementing real integration
     */
    fun startActor(actorId: String) {
        updateActorState(actorId, ActorState.Running) { actor ->
            println("Started actor: $actorId")
        }
    }
    
    /**
     * Stops an actor by updating its state to Stopped in the StateFlow
     * TODO: Connect to Actor.stop() when implementing real integration
     */
    fun stopActor(actorId: String) {
        updateActorState(actorId, ActorState.Stopped) { actor ->
            println("Stopped actor: $actorId")
        }
    }
    
    /**
     * Pauses an actor by updating its state to Paused in the StateFlow
     * TODO: Connect to Actor.pause() when implementing real integration
     */
    fun pauseActor(actorId: String) {
        updateActorState(actorId, ActorState.Paused("User requested pause")) { actor ->
            println("Paused actor: $actorId")
        }
    }
    
    /**
     * Resumes a paused actor by updating its state to Running in the StateFlow
     * TODO: Connect to Actor.resume() when implementing real integration
     */
    fun resumeActor(actorId: String) {
        val currentActors = _actors.value
        val actor = currentActors.find { it.id == actorId }
        
        if (actor != null && actor.state is ActorState.Paused) {
            updateActorState(actorId, ActorState.Running) { _ ->
                println("Resumed actor: $actorId")
            }
        } else if (actor == null) {
            println("Actor not found for resume: $actorId")
        } else {
            println("Actor $actorId is not in paused state, current state: ${actor.state}")
        }
    }
    
    /**
     * Helper method to update actor state in the StateFlow
     * @param actorId The ID of the actor to update
     * @param newState The new state to set
     * @param onSuccess Callback executed when the state is successfully updated
     */
    private fun updateActorState(
        actorId: String, 
        newState: ActorState, 
        onSuccess: (ActorDisplayData) -> Unit
    ) {
        val current = actorStore[actorId]
        if (current != null) {
            val updatedActor = current.copy(
                state = newState,
                lastUpdate = Clock.System.now()
            )
            actorStore[actorId] = updatedActor
            _actors.value = actorStore.values.toList()
            onSuccess(updatedActor)
        } else {
            println("Actor not found for state update: $actorId")
        }

    }
    
    companion object {
        private const val UPDATE_INTERVAL_MS = 2000L // 2 second updates for demo
        
        // Stable UUID-based IDs for demo actors to ensure consistency across UI updates
        // Using predictable UUIDs for demo purposes while maintaining uniqueness guarantees
        private val DEMO_ACTOR_MESSAGE_PROCESSOR = "a1e2f3d4-1234-4abc-8def-1234567890ab"
        private val DEMO_ACTOR_DATA_TRANSFORMER = "b2f3e4c5-2345-4bcd-9abc-2345678901bc"
        private val DEMO_ACTOR_RESULT_AGGREGATOR = "c3d4f5e6-3456-4cde-8bcd-3456789012cd"
        private val DEMO_ACTOR_LOGGER_SERVICE = "d4e5c6f7-4567-4def-9cde-4567890123de"
        
        // Stable UUID-based IDs for demo channels
        private val DEMO_CHANNEL_INPUT = "e5f6d7c8-5678-4abc-8def-5678901234ef"
        private val DEMO_CHANNEL_PROCESSING = "f6c7e8d9-6789-4bcd-9abc-6789012345fa"
        private val DEMO_CHANNEL_AGGREGATION = "a7d8f9e0-7890-4cde-8bcd-7890123456ab" 
        private val DEMO_CHANNEL_LOGGING = "b8e9c0f1-8901-4def-9cde-8901234567bc"
    }
}
