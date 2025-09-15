package org.solace.composeapp.ui.service

import org.solace.composeapp.actor.ActorState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlin.math.roundToInt
import org.solace.composeapp.ui.data.ActorDisplayData
import org.solace.composeapp.ui.data.ActorMetricsData
import org.solace.composeapp.ui.data.SystemMetricsData

/**
 * Service that provides real-time updates for the actor system UI
 */
class RealTimeActorService(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _actors = MutableStateFlow<List<ActorDisplayData>>(emptyList())
    val actors: StateFlow<List<ActorDisplayData>> = _actors.asStateFlow()
    
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
                id = "actor-1",
                name = "Sample Actor 1",
                state = ActorState.Running,
                lastUpdate = currentTime,
                metrics = ActorMetricsData(
                    messagesReceived = (0..1000).random().toLong(),
                    messagesProcessed = (0..800).random().toLong(),
                    messagesFailed = (0..50).random().toLong(),
                    successRate = (85..99).random().toDouble(),
                    averageProcessingTime = (10..100).random().toDouble(),
                    lastProcessingTime = (5..50).random().toLong()
                )
            ),
            ActorDisplayData(
                id = "actor-2",
                name = "Sample Actor 2",
                state = ActorState.Paused("Maintenance"),
                lastUpdate = currentTime,
                metrics = ActorMetricsData(
                    messagesReceived = (0..500).random().toLong(),
                    messagesProcessed = (0..400).random().toLong(),
                    messagesFailed = (0..20).random().toLong(),
                    successRate = (90..100).random().toDouble(),
                    averageProcessingTime = (15..80).random().toDouble(),
                    lastProcessingTime = (8..30).random().toLong()
                )
            ),
            ActorDisplayData(
                id = "actor-3",
                name = "Sample Actor 3",
                state = ActorState.Error("Connection timeout"),
                lastUpdate = currentTime,
                metrics = ActorMetricsData(
                    messagesReceived = (0..200).random().toLong(),
                    messagesProcessed = (0..100).random().toLong(),
                    messagesFailed = (10..80).random().toLong(),
                    successRate = (30..70).random().toDouble(),
                    averageProcessingTime = (50..200).random().toDouble(),
                    lastProcessingTime = (100..500).random().toLong()
                )
            )
        )
        
        _actors.value = sampleActors
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
     * These methods provide mock behavior for managing actors in the UI.
     * TODO: In a real implementation, these would interface with the actual SolaceCore actor system.
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
        
        _actors.value = _actors.value + newActor
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
            _actors.value = currentActors.filter { it.id != actorId }
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
        val currentActors = _actors.value
        val actorIndex = currentActors.indexOfFirst { it.id == actorId }
        
        if (actorIndex != -1) {
            val updatedActor = currentActors[actorIndex].copy(
                state = newState,
                lastUpdate = Clock.System.now()
            )
            
            val updatedActors = currentActors.toMutableList()
            updatedActors[actorIndex] = updatedActor
            _actors.value = updatedActors
            
            onSuccess(updatedActor)
        } else {
            println("Actor not found for state update: $actorId")
        }
    }
    
    companion object {
        private const val UPDATE_INTERVAL_MS = 1000L // 1 second updates
    }
}