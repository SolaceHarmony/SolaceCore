package org.solace.composeapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.solace.composeapp.ui.components.*
import org.solace.composeapp.ui.service.RealTimeActorService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme {
        SolaceRealTimeUI()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolaceRealTimeUI() {
    val actorService = remember { RealTimeActorService() }
    val actors by actorService.actors.collectAsState()
    val channels by actorService.channels.collectAsState()
    val selectedWorkflow by actorService.selectedWorkflow.collectAsState()
    val systemMetrics by actorService.systemMetrics.collectAsState()
    val isMonitoring by actorService.isMonitoring.collectAsState()
    
    var selectedActorId by remember { mutableStateOf<String?>(null) }
    val selectedActor = actors.find { it.id == selectedActorId }
    
    LaunchedEffect(Unit) {
        actorService.startMonitoring()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            actorService.stopMonitoring()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("SolaceCore Real-Time Monitor") 
                },
                actions = {
                    RealTimeStatusIndicator(isConnected = isMonitoring)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // System metrics dashboard
            SystemMetricsDashboard(metrics = systemMetrics)
            
            // Monitoring controls
            MonitoringControlPanel(
                isMonitoring = isMonitoring,
                onStartStop = {
                    if (isMonitoring) {
                        actorService.stopMonitoring()
                    } else {
                        actorService.startMonitoring()
                    }
                },
                onRefresh = {
                    // Force refresh by stopping and starting
                    actorService.stopMonitoring()
                    actorService.startMonitoring()
                }
            )
            
            // Two-column layout for better organization
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left column: Actor and Channel monitoring
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Actor list with clickable items
                    ActorListView(
                        actors = actors,
                        selectedActorId = selectedActorId,
                        onActorSelected = { actorId -> 
                            selectedActorId = if (selectedActorId == actorId) null else actorId 
                        }
                    )
                    
                    // Channel monitoring
                    ChannelMonitoringView(channels = channels)
                }
                
                // Right column: Controls and Visualization
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Actor control panel
                    ActorControlPanel(
                        selectedActor = selectedActor,
                        onCreateActor = { actorService.createActor() },
                        onDeleteActor = { actorId -> actorService.deleteActor(actorId) },
                        onStartActor = { actorId -> actorService.startActor(actorId) },
                        onStopActor = { actorId -> actorService.stopActor(actorId) },
                        onPauseActor = { actorId -> actorService.pauseActor(actorId) },
                        onResumeActor = { actorId -> actorService.resumeActor(actorId) }
                    )
                    
                    // Workflow visualization
                    WorkflowVisualizationView(workflow = selectedWorkflow)
                    
                    // Quick action buttons
                    QuickActionButtons(
                        onStartAll = { 
                            actors.forEach { actor -> 
                                actorService.startActor(actor.id) 
                            }
                        },
                        onStopAll = { 
                            actors.forEach { actor -> 
                                actorService.stopActor(actor.id) 
                            }
                        },
                        onPauseAll = { 
                            actors.forEach { actor -> 
                                actorService.pauseActor(actor.id) 
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
expect fun PlatformText(text: String)
