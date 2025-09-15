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
    val systemMetrics by actorService.systemMetrics.collectAsState()
    val isMonitoring by actorService.isMonitoring.collectAsState()
    
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
            
            // Actor list
            ActorListView(actors = actors)
            
            // Actor control panel for lifecycle management
            ActorControlPanel(
                actors = actors,
                actorService = actorService
            )
        }
    }
}

@Composable
expect fun PlatformText(text: String)
