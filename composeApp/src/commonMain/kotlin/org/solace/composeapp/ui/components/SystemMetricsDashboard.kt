package org.solace.composeapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.solace.composeapp.ui.data.SystemMetricsData
import kotlin.math.roundToInt

/**
 * Dashboard component displaying system-wide metrics
 */
@Composable
fun SystemMetricsDashboard(
    metrics: SystemMetricsData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "System Overview",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SystemMetricCard(
                    title = "Total Actors",
                    value = metrics.totalActors.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                
                SystemMetricCard(
                    title = "Running",
                    value = metrics.runningActors.toString(),
                    color = Color(0xFF4CAF50)
                )
                
                SystemMetricCard(
                    title = "Errors",
                    value = metrics.errorActors.toString(),
                    color = Color(0xFFF44336)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SystemMetricCard(
                    title = "Messages",
                    value = formatLargeNumber(metrics.totalMessages),
                    color = MaterialTheme.colorScheme.secondary
                )
                
                SystemMetricCard(
                    title = "Avg Response",
                    value = "${((metrics.averageResponseTime * 10).roundToInt() / 10.0)}ms",
                    color = MaterialTheme.colorScheme.tertiary
                )
                
                SystemMetricCard(
                    title = "Uptime",
                    value = formatUptime(metrics.systemUptime),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * Individual metric card component
 */
@Composable
fun SystemMetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        modifier = modifier.width(120.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Real-time status indicator
 */
@Composable
fun RealTimeStatusIndicator(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .fillMaxSize()
        ) {
            Surface(
                color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxSize()
            ) {}
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = if (isConnected) "Real-time Connected" else "Disconnected",
            style = MaterialTheme.typography.labelMedium,
            color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
        )
    }
}

/**
 * Control panel for monitoring
 */
@Composable
fun MonitoringControlPanel(
    isMonitoring: Boolean,
    onStartStop: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Monitoring Controls",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row {
                Button(
                    onClick = onStartStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMonitoring) Color(0xFFF44336) else Color(0xFF4CAF50)
                    )
                ) {
                    Text(if (isMonitoring) "Stop" else "Start")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                OutlinedButton(onClick = onRefresh) {
                    Text("Refresh")
                }
            }
        }
    }
}

/**
 * Format large numbers with K, M suffixes
 */
private fun formatLargeNumber(number: Long): String = when {
    number >= 1_000_000 -> "${((number / 1_000_000.0 * 10).roundToInt() / 10.0)}M"
    number >= 1_000 -> "${((number / 1_000.0 * 10).roundToInt() / 10.0)}K"
    else -> number.toString()
}

/**
 * Format uptime in a human-readable format
 */
private fun formatUptime(uptimeMs: Long): String {
    val seconds = uptimeMs / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        days > 0 -> "${days}d ${hours % 24}h"
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}
