package org.solace.composeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import org.solace.composeapp.ui.components.*
import org.solace.composeapp.ui.graph.ActorGraphView
import org.solace.composeapp.ui.service.RealTimeActorService
import org.solace.composeapp.ui.theme.SolaceTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    SolaceTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxSize()
                        .widthIn(max = 1320.dp),
                    tonalElevation = 4.dp,
                    shadowElevation = 16.dp,
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ) {
                    SolaceRealTimeUI()
                }
            }
        }
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
                .fillMaxSize(),
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
            // Interactive Actor Graph View
            ActorGraphView(
                actors = actors,
                channels = channels,
                actorService = actorService
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
                        actors = actors,
                        actorService = actorService
                    )

                    // Workflow visualization
                    WorkflowVisualizationView(workflow = selectedWorkflow)
                }
            }
            
        }
    }
}

@Composable
expect fun PlatformText(text: String)
