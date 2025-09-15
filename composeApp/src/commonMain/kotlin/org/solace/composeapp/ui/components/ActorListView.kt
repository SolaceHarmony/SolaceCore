package org.solace.composeapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.solace.composeapp.actor.ActorState
import org.solace.composeapp.ui.data.ActorDisplayData
import kotlin.math.roundToInt

/**
 * Component that displays a list of actors with their current state and metrics
 */
@Composable
fun ActorListView(
    actors: List<ActorDisplayData>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Active Actors (${actors.size})",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(actors) { actor ->
                    ActorItemView(actor = actor)
                }
            }
        }
    }
}

/**
 * Individual actor item component
 */
@Composable
fun ActorItemView(
    actor: ActorDisplayData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = actor.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "ID: ${actor.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                ActorStatusBadge(state = actor.state)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ActorMetricsView(metrics = actor.metrics)
        }
    }
}

/**
 * Badge showing the current actor state
 */
@Composable
fun ActorStatusBadge(
    state: ActorState,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (state) {
        is ActorState.Running -> Color(0xFF4CAF50) to "Running"
        is ActorState.Stopped -> Color(0xFF9E9E9E) to "Stopped"
        is ActorState.Error -> Color(0xFFF44336) to "Error"
        is ActorState.Paused -> Color(0xFFFF9800) to "Paused"
        is ActorState.Initialized -> Color(0xFF2196F3) to "Initialized"
    }
    
    Surface(
        color = color,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Displays actor metrics in a compact format
 */
@Composable
fun ActorMetricsView(
    metrics: org.solace.composeapp.ui.data.ActorMetricsData,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricItem(
                label = "Received",
                value = metrics.messagesReceived.toString()
            )
            MetricItem(
                label = "Processed",
                value = metrics.messagesProcessed.toString()
            )
            MetricItem(
                label = "Failed",
                value = metrics.messagesFailed.toString()
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricItem(
                label = "Success Rate",
                value = "${((metrics.successRate * 10).roundToInt() / 10.0)}%"
            )
            MetricItem(
                label = "Avg Time",
                value = "${((metrics.averageProcessingTime * 10).roundToInt() / 10.0)}ms"
            )
        }
    }
}

/**
 * Individual metric display item
 */
@Composable
fun MetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}