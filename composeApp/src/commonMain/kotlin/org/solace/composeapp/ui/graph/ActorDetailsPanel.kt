package org.solace.composeapp.ui.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.solace.composeapp.actor.ActorState
import org.solace.composeapp.ui.data.ActorDisplayData
import org.solace.composeapp.ui.service.RealTimeActorService

/**
 * Details panel for selected actor showing KPIs, state, and lifecycle controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActorDetailsPanel(
    actor: ActorDisplayData?,
    actorService: RealTimeActorService,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (actor == null) return
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 600.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = actor.name,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "ID: ${actor.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // State badge
            ActorStateBadge(state = actor.state)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Lifecycle controls
            LifecycleControls(actor = actor, actorService = actorService)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            HorizontalDivider()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // KPIs section
            Text(
                text = "Key Performance Indicators",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            KPICards(actor = actor)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mini charts section
            Text(
                text = "Performance Metrics",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            MiniCharts(actor = actor)
        }
    }
}

/**
 * State badge showing current actor state
 */
@Composable
private fun ActorStateBadge(state: ActorState) {
    val (color, text) = when (state) {
        is ActorState.Running -> Color(0xFF4CAF50) to "Running"
        is ActorState.Stopped -> Color(0xFF9E9E9E) to "Stopped"
        is ActorState.Error -> Color(0xFFF44336) to "Error: ${state.exception}"
        is ActorState.Paused -> Color(0xFFFF9800) to "Paused: ${state.reason}"
        is ActorState.Initialized -> Color(0xFF2196F3) to "Initialized"
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, MaterialTheme.shapes.small)
            )
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/**
 * Lifecycle control buttons
 */
@Composable
private fun LifecycleControls(
    actor: ActorDisplayData,
    actorService: RealTimeActorService
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Start button
        Button(
            onClick = { actorService.startActor(actor.id) },
            enabled = actor.state !is ActorState.Running,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Start")
        }
        
        // Pause button
        Button(
            onClick = { actorService.pauseActor(actor.id) },
            enabled = actor.state is ActorState.Running,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Pause")
        }
        
        // Resume button
        Button(
            onClick = { actorService.resumeActor(actor.id) },
            enabled = actor.state is ActorState.Paused,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Resume")
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Stop button
        OutlinedButton(
            onClick = { actorService.stopActor(actor.id) },
            enabled = actor.state !is ActorState.Stopped,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Stop")
        }
        
        // Delete button
        OutlinedButton(
            onClick = { actorService.deleteActor(actor.id) },
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Delete")
        }
    }
}

/**
 * KPI metric cards
 */
@Composable
private fun KPICards(actor: ActorDisplayData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KPICard(
            label = "Received",
            value = actor.metrics.messagesReceived.toString(),
            modifier = Modifier.weight(1f)
        )
        KPICard(
            label = "Processed",
            value = actor.metrics.messagesProcessed.toString(),
            modifier = Modifier.weight(1f)
        )
        KPICard(
            label = "Failed",
            value = actor.metrics.messagesFailed.toString(),
            color = if (actor.metrics.messagesFailed > 0) Color(0xFFF44336) else null,
            modifier = Modifier.weight(1f)
        )
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KPICard(
            label = "Success Rate",
            value = "${(actor.metrics.successRate * 10).toInt() / 10.0}%",
            color = when {
                actor.metrics.successRate >= 95 -> Color(0xFF4CAF50)
                actor.metrics.successRate >= 80 -> Color(0xFFFF9800)
                else -> Color(0xFFF44336)
            },
            modifier = Modifier.weight(1f)
        )
        KPICard(
            label = "Avg Time",
            value = "${(actor.metrics.averageProcessingTime * 10).toInt() / 10.0}ms",
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Individual KPI card
 */
@Composable
private fun KPICard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = color ?: MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Mini charts showing performance over time
 */
@Composable
private fun MiniCharts(actor: ActorDisplayData) {
    // Simple bar chart representations
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MiniBarChart(
            label = "Throughput",
            value = actor.metrics.messagesProcessed.toFloat(),
            maxValue = actor.metrics.messagesReceived.toFloat().coerceAtLeast(1f),
            color = Color(0xFF4CAF50)
        )
        
        MiniBarChart(
            label = "Latency",
            value = actor.metrics.averageProcessingTime.toFloat(),
            maxValue = 200f,
            color = Color(0xFF2196F3)
        )
        
        MiniBarChart(
            label = "Error Rate",
            value = actor.metrics.messagesFailed.toFloat(),
            maxValue = actor.metrics.messagesReceived.toFloat().coerceAtLeast(1f),
            color = Color(0xFFF44336)
        )
    }
}

/**
 * Simple horizontal bar chart
 */
@Composable
private fun MiniBarChart(
    label: String,
    value: Float,
    maxValue: Float,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value.toInt().toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = { (value / maxValue).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}
