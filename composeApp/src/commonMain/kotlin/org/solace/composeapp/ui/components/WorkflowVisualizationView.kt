package org.solace.composeapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.solace.composeapp.ui.data.WorkflowDisplayData
import org.solace.composeapp.ui.data.WorkflowState
import org.solace.composeapp.ui.data.ActorDisplayData
import org.solace.composeapp.ui.data.ChannelDisplayData
import org.solace.composeapp.actor.ActorState

/**
 * Component that visualizes workflows with actors and channels
 */
@Composable
fun WorkflowVisualizationView(
    workflow: WorkflowDisplayData?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Workflow Visualization",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                if (workflow != null) {
                    WorkflowStatusBadge(state = workflow.state)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (workflow != null) {
                WorkflowDiagram(workflow = workflow)
            } else {
                EmptyWorkflowState()
            }
        }
    }
}

/**
 * Badge showing the current workflow state
 */
@Composable
fun WorkflowStatusBadge(
    state: WorkflowState,
    modifier: Modifier = Modifier
) {
    val (color, text, icon) = when (state) {
        is WorkflowState.Running -> Triple(Color(0xFF4CAF50), "Running", Icons.Default.PlayArrow)
        is WorkflowState.Stopped -> Triple(Color(0xFF9E9E9E), "Stopped", Icons.Default.PlayArrow)
        is WorkflowState.Paused -> Triple(Color(0xFFFF9800), "Paused", Icons.Default.PlayArrow)
        is WorkflowState.Error -> Triple(Color(0xFFF44336), "Error", Icons.Default.PlayArrow)
        is WorkflowState.Initialized -> Triple(Color(0xFF2196F3), "Initialized", Icons.Default.PlayArrow)
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
 * Visual diagram of the workflow
 */
@Composable
fun WorkflowDiagram(
    workflow: WorkflowDisplayData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawWorkflowConnections(workflow.channels)
        }
        
        // Position actors in a grid layout
        val actorPositions = calculateActorPositions(workflow.actors.size)
        
        workflow.actors.forEachIndexed { index, actor ->
            val position = actorPositions.getOrNull(index) ?: Offset(0.5f, 0.5f)
            
            ActorNode(
                actor = actor,
                modifier = Modifier
                    .offset(
                        x = (position.x * 300).dp,
                        y = (position.y * 200).dp
                    )
                    .size(80.dp)
            )
        }
    }
}

/**
 * Individual actor node in the workflow diagram
 */
@Composable
fun ActorNode(
    actor: ActorDisplayData,
    modifier: Modifier = Modifier
) {
    val stateColor = when (actor.state) {
        is ActorState.Running -> Color(0xFF4CAF50)
        is ActorState.Stopped -> Color(0xFF9E9E9E)
        is ActorState.Error -> Color(0xFFF44336)
        is ActorState.Paused -> Color(0xFFFF9800)
        is ActorState.Initialized -> Color(0xFF2196F3)
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(stateColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(stateColor)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = actor.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Empty state when no workflow is selected
 */
@Composable
fun EmptyWorkflowState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No Workflow Selected",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Select a workflow to view its visualization",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Calculate positions for actors in a grid layout
 */
private fun calculateActorPositions(actorCount: Int): List<Offset> {
    if (actorCount == 0) return emptyList()
    
    val gridSize = kotlin.math.ceil(kotlin.math.sqrt(actorCount.toDouble())).toInt()
    val positions = mutableListOf<Offset>()
    
    for (i in 0 until actorCount) {
        val row = i / gridSize
        val col = i % gridSize
        val x = (col + 0.5f) / gridSize
        val y = (row + 0.5f) / gridSize
        positions.add(Offset(x, y))
    }
    
    return positions
}

/**
 * Draw connections between actors based on channels and their positions
 * @param channels List of channels to draw
 * @param actorPositions Map of actorId to normalized Offset (x, y in [0,1])
 */
private fun DrawScope.drawWorkflowConnections(
    channels: List<ChannelDisplayData>,
    actorPositions: Map<String, Offset>
) {
    channels.forEach { channel ->
        val connectionColor = when (channel.connectionState) {
            is org.solace.composeapp.ui.data.ChannelConnectionState.Connected -> Color(0xFF4CAF50)
            is org.solace.composeapp.ui.data.ChannelConnectionState.Error -> Color(0xFFF44336)
            else -> Color(0xFF9E9E9E)
        }
        val sourcePos = actorPositions[channel.sourceActorId]
        val targetPos = actorPositions[channel.targetActorId]
        if (sourcePos != null && targetPos != null) {
            // Scale normalized positions to canvas size
            val start = Offset(sourcePos.x * size.width, sourcePos.y * size.height)
            val end = Offset(targetPos.x * size.width, targetPos.y * size.height)
            drawLine(
                color = connectionColor,
                start = start,
                end = end,
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}