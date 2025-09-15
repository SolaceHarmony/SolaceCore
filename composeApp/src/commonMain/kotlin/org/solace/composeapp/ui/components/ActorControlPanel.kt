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
import org.solace.composeapp.ui.service.RealTimeActorService

/**
 * Component for managing actor lifecycle operations
 * Provides buttons to create, delete, start, stop, pause, and resume actors
 */
@Composable
fun ActorControlPanel(
    actors: List<ActorDisplayData>,
    actorService: RealTimeActorService,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Actor Control Panel",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Global actor actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { actorService.createActor() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("Create Actor")
                }
                
                OutlinedButton(
                    onClick = { 
                        // Start all stopped/initialized actors
                        actors.filter { 
                            it.state is ActorState.Stopped || it.state is ActorState.Initialized 
                        }.forEach { actor ->
                            actorService.startActor(actor.id)
                        }
                    }
                ) {
                    Text("Start All")
                }
                
                OutlinedButton(
                    onClick = { 
                        // Stop all running actors
                        actors.filter { it.state is ActorState.Running }.forEach { actor ->
                            actorService.stopActor(actor.id)
                        }
                    }
                ) {
                    Text("Stop All")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Individual actor controls
            if (actors.isNotEmpty()) {
                Text(
                    text = "Individual Actor Controls",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(actors) { actor ->
                        ActorControlItem(
                            actor = actor,
                            actorService = actorService
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No actors available. Create one to get started!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Individual actor control component with action buttons
 */
@Composable
fun ActorControlItem(
    actor: ActorDisplayData,
    actorService: RealTimeActorService,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
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
                        style = MaterialTheme.typography.titleSmall
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
            
            // Action buttons based on current state
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                when (actor.state) {
                    is ActorState.Initialized -> {
                        Button(
                            onClick = { actorService.startActor(actor.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Start")
                        }
                    }
                    
                    is ActorState.Running -> {
                        Button(
                            onClick = { actorService.pauseActor(actor.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pause")
                        }
                        Button(
                            onClick = { actorService.stopActor(actor.id) },
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
                            onClick = { actorService.resumeActor(actor.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Resume")
                        }
                        Button(
                            onClick = { actorService.stopActor(actor.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Stop")
                        }
                    }
                    
                    is ActorState.Stopped -> {
                        Button(
                            onClick = { actorService.startActor(actor.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Restart")
                        }
                    }
                    
                    is ActorState.Error -> {
                        Button(
                            onClick = { actorService.startActor(actor.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Restart")
                        }
                    }
                }
                
                // Delete button (always available)
                OutlinedButton(
                    onClick = { actorService.deleteActor(actor.id) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF44336)
                    ),
                    modifier = Modifier.weight(0.7f)
                ) {
                    Text("Delete")
                }
            }
        }
    }
}