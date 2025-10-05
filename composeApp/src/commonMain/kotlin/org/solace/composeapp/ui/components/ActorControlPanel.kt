package org.solace.composeapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.solace.composeapp.actor.ActorState
import org.solace.composeapp.ui.data.ActorDisplayData
import org.solace.composeapp.ui.service.RealTimeActorService

/**
 * Actor control panel listing global and per-actor lifecycle controls.
 */
@Composable
fun ActorControlPanel(
    actors: List<ActorDisplayData>,
    actorService: RealTimeActorService,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Actor Control Panel", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(16.dp))

            // Global actor actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { actorService.createActor() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Create Actor")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Actor")
                }

                OutlinedButton(
                    onClick = {
                        // Start all stopped/initialized actors
                        actors.filter { it.state is ActorState.Stopped || it.state is ActorState.Initialized }
                            .forEach { actor -> actorService.startActor(actor.id) }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Start All") }

                OutlinedButton(
                    onClick = {
                        // Stop all running actors
                        actors.filter { it.state is ActorState.Running }
                            .forEach { actor -> actorService.stopActor(actor.id) }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Stop All") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Individual Actor Controls", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(8.dp))

            if (actors.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No actors available. Create one to get started!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(actors) { actor ->
                        ActorRow(actor = actor, actorService = actorService)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActorRow(
    actor: ActorDisplayData,
    actorService: RealTimeActorService,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = actor.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "ID: ${actor.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (actor.state) {
                    is ActorState.Stopped, is ActorState.Initialized -> {
                        Button(
                            onClick = { actorService.startActor(actor.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start")
                        }
                    }
                    is ActorState.Running -> {
                        Button(
                            onClick = { actorService.pauseActor(actor.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Pause") }
                        Button(
                            onClick = { actorService.stopActor(actor.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Stop") }
                    }
                    is ActorState.Paused -> {
                        Button(
                            onClick = { actorService.resumeActor(actor.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Resume")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume")
                        }
                        Button(
                            onClick = { actorService.stopActor(actor.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Stop") }
                    }
                    is ActorState.Error -> {
                        Button(
                            onClick = { actorService.startActor(actor.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Restart")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restart")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(onClick = { actorService.deleteActor(actor.id) }, modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Actor")
            }
        }
    }
}

/** Quick action buttons used by the parent layout. */
@Composable
fun QuickActionButtons(
    onStartAll: () -> Unit,
    onStopAll: () -> Unit,
    onPauseAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Quick Actions", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStartAll,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start All")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start All")
                }
                Button(
                    onClick = onPauseAll,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    modifier = Modifier.weight(1f)
                ) { Text("Pause All") }
                Button(
                    onClick = onStopAll,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                    modifier = Modifier.weight(1f)
                ) { Text("Stop All") }
            }
        }
    }
}

