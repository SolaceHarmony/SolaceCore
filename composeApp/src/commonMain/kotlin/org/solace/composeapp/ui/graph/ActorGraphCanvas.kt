package org.solace.composeapp.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.solace.composeapp.actor.ActorState
import org.solace.composeapp.ui.data.ActorDisplayData
import org.solace.composeapp.ui.data.ChannelDisplayData
import org.solace.composeapp.ui.data.ChannelConnectionState

/**
 * Interactive graph canvas with pan/zoom capabilities
 * Displays actors as nodes and channels as edges
 */
@Composable
fun ActorGraphCanvas(
    actors: List<ActorDisplayData>,
    channels: List<ChannelDisplayData>,
    selectedActorId: String?,
    onActorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    
    // Calculate layout when graph topology changes (actor IDs or channel connections)
    val nodePositions by remember(
        actors.map { it.id },
        channels.map { it.sourceActorId to it.targetActorId }
    ) {
        derivedStateOf {
            val layout = ForceDirectedLayout(
                width = 800f,
                height = 600f,
                iterations = 50
            )
            val edges = channels.map { 
                ForceDirectedLayout.Edge(it.sourceActorId, it.targetActorId) 
            }
            layout.calculateLayout(actors.map { it.id }, edges)
        }
    }
    
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 3f)
        offsetX += panChange.x
        offsetY += panChange.y
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .transformable(state = transformableState)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        ) {
            // Draw edges first (behind nodes)
            channels.forEach { channel ->
                val sourcePos = nodePositions[channel.sourceActorId]
                val targetPos = nodePositions[channel.targetActorId]
                
                if (sourcePos != null && targetPos != null) {
                    val isHighlighted = selectedActorId == channel.sourceActorId || 
                                      selectedActorId == channel.targetActorId
                    drawEdge(
                        sourcePos = sourcePos,
                        targetPos = targetPos,
                        channel = channel,
                        isHighlighted = isHighlighted
                    )
                }
            }
        }
        
        // Draw nodes on top
        actors.forEach { actor ->
            val position = nodePositions[actor.id]
            if (position != null) {
                ActorGraphNode(
                    actor = actor,
                    position = position,
                    scale = scale,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    isSelected = selectedActorId == actor.id,
                    onSelected = { onActorSelected(actor.id) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Draw an edge between two nodes
 */
private fun DrawScope.drawEdge(
    sourcePos: Offset,
    targetPos: Offset,
    channel: ChannelDisplayData,
    isHighlighted: Boolean
) {
    val start = Offset(sourcePos.x * size.width, sourcePos.y * size.height)
    val end = Offset(targetPos.x * size.width, targetPos.y * size.height)
    
    val color = when {
        isHighlighted -> Color(0xFF2196F3) // Highlighted blue
        channel.connectionState is ChannelConnectionState.Connected -> Color(0xFF4CAF50)
        channel.connectionState is ChannelConnectionState.Error -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }
    
    val strokeWidth = if (isHighlighted) 3.dp.toPx() else 2.dp.toPx()
    
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth
    )
}

/**
 * Individual node in the graph
 */
@Composable
fun ActorGraphNode(
    actor: ActorDisplayData,
    position: Offset,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stateColor = when (actor.state) {
        is ActorState.Running -> Color(0xFF4CAF50)
        is ActorState.Stopped -> Color(0xFF9E9E9E)
        is ActorState.Error -> Color(0xFFF44336)
        is ActorState.Paused -> Color(0xFFFF9800)
        is ActorState.Initialized -> Color(0xFF2196F3)
    }
    
    BoxWithConstraints(modifier = modifier) {
        val canvasWidth = maxWidth.value
        val canvasHeight = maxHeight.value
        
        val nodeX = position.x * canvasWidth * scale + offsetX
        val nodeY = position.y * canvasHeight * scale + offsetY
        
        Column(
            modifier = Modifier
                .offset(x = nodeX.dp, y = nodeY.dp)
                .size(80.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = "Actor ${actor.name}, state: ${actor.state}"
                }
                .clickable(onClick = onSelected),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) 
                            stateColor.copy(alpha = 0.4f)
                        else 
                            stateColor.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(stateColor)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = actor.name,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
