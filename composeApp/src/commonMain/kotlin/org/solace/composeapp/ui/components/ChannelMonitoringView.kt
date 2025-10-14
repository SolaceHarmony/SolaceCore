package org.solace.composeapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.solace.composeapp.ui.data.ChannelDisplayData
import org.solace.composeapp.ui.data.ChannelConnectionState
import org.solace.composeapp.ui.data.ChannelMetricsData

/**
 * Component that displays a list of channels with their current state and metrics
 */
@Composable
fun ChannelMonitoringView(
    channels: List<ChannelDisplayData>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Active Channels (${channels.size})",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(channels) { channel ->
                    ChannelItemView(channel = channel)
                }
            }
        }
    }
}

/**
 * Individual channel item component
 */
@Composable
fun ChannelItemView(
    channel: ChannelDisplayData,
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
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${channel.sourceActorId} → ${channel.targetActorId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Type: ${channel.type}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                ChannelStatusBadge(state = channel.connectionState)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ChannelMetricsView(metrics = channel.metrics)
        }
    }
}

/**
 * Badge showing the current channel connection state
 */
@Composable
fun ChannelStatusBadge(
    state: ChannelConnectionState,
    modifier: Modifier = Modifier
) {
    val (color, text, icon) = when (state) {
        is ChannelConnectionState.Connected -> Triple(Color(0xFF4CAF50), "Connected", Icons.Default.CheckCircle)
        is ChannelConnectionState.Disconnected -> Triple(Color(0xFF9E9E9E), "Disconnected", Icons.Default.CheckCircle)
        is ChannelConnectionState.Connecting -> Triple(Color(0xFFFF9800), "Connecting", Icons.Default.CheckCircle)
        is ChannelConnectionState.Error -> Triple(Color(0xFFF44336), "Error", Icons.Default.CheckCircle)
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * Displays channel metrics in a compact format
 */
@Composable
fun ChannelMetricsView(
    metrics: ChannelMetricsData,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricItem(
                label = "Sent",
                value = formatMetricNumber(metrics.messagesSent)
            )
            MetricItem(
                label = "Received",
                value = formatMetricNumber(metrics.messagesReceived)
            )
            MetricItem(
                label = "Dropped",
                value = formatMetricNumber(metrics.messagesDropped)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricItem(
                label = "Latency",
                value = "${(metrics.averageLatency * 10).toInt() / 10.0}ms"
            )
            MetricItem(
                label = "Throughput",
                value = "${(metrics.throughputPerSecond * 10).toInt() / 10.0}/s"
            )
            MetricItem(
                label = "Error Rate",
                value = "${(metrics.errorRate * 10).toInt() / 10.0}%"
            )
        }
    }
}

/**
 * Format large numbers with K, M suffixes for metrics
 */
private fun formatMetricNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> "${(number / 100_000).toInt() / 10.0}M"
        number >= 1_000 -> "${(number / 100).toInt() / 10.0}K"
        else -> number.toString()
    }
}