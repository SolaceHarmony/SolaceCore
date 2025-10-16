package org.solace.composeapp.ui.graph

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.solace.composeapp.ui.data.ActorDisplayData
import org.solace.composeapp.ui.data.ChannelDisplayData
import org.solace.composeapp.ui.service.RealTimeActorService

/**
 * Main interactive actor graph view component
 * Shows actors as nodes and channels as edges with pan/zoom capabilities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActorGraphView(
    actors: List<ActorDisplayData>,
    channels: List<ChannelDisplayData>,
    actorService: RealTimeActorService,
    modifier: Modifier = Modifier
) {
    var selectedActorId by remember { mutableStateOf<String?>(null) }
    val selectedActor = actors.find { it.id == selectedActorId }
    
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Actor Graph",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "${actors.size} actors, ${channels.size} channels",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Info icon
                    IconButton(onClick = { /* Could show help dialog */ }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Graph info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Fit to view button
                    IconButton(onClick = { /* Could reset zoom/pan */ }) {
                        Icon(
                            Icons.Default.CenterFocusWeak,
                            contentDescription = "Fit to view",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Graph canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                if (actors.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No actors to display",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Start monitoring to see the actor graph",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        ActorGraphCanvas(
                            actors = actors,
                            channels = channels,
                            selectedActorId = selectedActorId,
                            onActorSelected = { actorId ->
                                selectedActorId = if (selectedActorId == actorId) null else actorId
                            }
                        )
                    }
                }
            }
            
            // Details panel (shown when actor is selected)
            if (selectedActor != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                ActorDetailsPanel(
                    actor = selectedActor,
                    actorService = actorService,
                    onClose = { selectedActorId = null }
                )
            }
            
            // Instructions
            if (actors.isNotEmpty() && selectedActor == null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "💡 Tip: Pan with drag, zoom with pinch/scroll, click nodes for details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
