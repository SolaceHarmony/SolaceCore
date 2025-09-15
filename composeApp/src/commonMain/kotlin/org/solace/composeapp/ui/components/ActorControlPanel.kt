package org.solace.composeapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.solace.composeapp.actor.ActorState
import org.solace.composeapp.ui.data.ActorDisplayData

/**
 * Enhanced control panel for actor lifecycle management
 */
@Composable
fun ActorControlPanel(
    selectedActor: ActorDisplayData?,
    onCreateActor: () -> Unit,
    onDeleteActor: (String) -> Unit,
    onStartActor: (String) -> Unit,
    onStopActor: (String) -> Unit,
    onPauseActor: (String) -> Unit,
    onResumeActor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Actor Management",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Global actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCreateActor,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Actor"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Actor")
                }
                
                OutlinedButton(
                    onClick = { /* Implement refresh all */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh All"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh All")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Actor-specific controls
            if (selectedActor != null) {
                ActorSpecificControls(
                    actor = selectedActor,
                    onDeleteActor = onDeleteActor,
                    onStartActor = onStartActor,
                    onStopActor = onStopActor,
                    onPauseActor = onPauseActor,
                    onResumeActor = onResumeActor
                )
            } else {
                EmptyActorSelection()
            }
        }
    }
}

/**
 * Controls specific to the selected actor
 */
@Composable
fun ActorSpecificControls(
    actor: ActorDisplayData,
    onDeleteActor: (String) -> Unit,
    onStartActor: (String) -> Unit,
    onStopActor: (String) -> Unit,
    onPauseActor: (String) -> Unit,
    onResumeActor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Selected Actor: ${actor.name}",
            style = MaterialTheme.typography.titleMedium
        )
        
        Text(
            text = "ID: ${actor.id}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Lifecycle controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (actor.state) {
                is ActorState.Stopped, is ActorState.Initialized -> {
                    Button(
                        onClick = { onStartActor(actor.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start")
                    }
                }
                is ActorState.Running -> {
                    Button(
                        onClick = { onPauseActor(actor.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pause")
                    }
                    
                    Button(
                        onClick = { onStopActor(actor.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Stop")
                    }
                }
                is ActorState.Paused -> {
                    Button(
                        onClick = { onResumeActor(actor.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resume")
                    }
                    
                    Button(
                        onClick = { onStopActor(actor.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Stop")
                    }
                }
                is ActorState.Error -> {
                    Button(
                        onClick = { onStartActor(actor.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restart")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Destructive actions
        OutlinedButton(
            onClick = { onDeleteActor(actor.id) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Delete Actor")
        }
    }
}

/**
 * Message when no actor is selected
 */
@Composable
fun EmptyActorSelection(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Select an actor to manage its lifecycle",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Lifecycle action buttons for quick access
 */
@Composable
fun QuickActionButtons(
    onStartAll: () -> Unit,
    onStopAll: () -> Unit,
    onPauseAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start All"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start All")
                }
                
                Button(
                    onClick = onPauseAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Pause All")
                }
                
                Button(
                    onClick = onStopAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop All")
                }
            }
        }
    }
}