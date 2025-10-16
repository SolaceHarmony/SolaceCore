package org.solace.composeapp.ui.graph

import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Force-directed graph layout using Fruchterman-Reingold algorithm
 * Computes 2D positions for nodes based on attractive and repulsive forces
 */
class ForceDirectedLayout(
    private val width: Float = 1000f,
    private val height: Float = 800f,
    private val iterations: Int = 50,
    private val initialTemp: Float = 100f,
    private val k: Float = 50f // Ideal spring length
) {
    
    data class Node(
        val id: String,
        var x: Float,
        var y: Float,
        var dx: Float = 0f,
        var dy: Float = 0f
    )
    
    data class Edge(
        val sourceId: String,
        val targetId: String
    )
    
    /**
     * Calculate positions for nodes using force-directed layout
     * Returns a map of node IDs to their calculated positions
     */
    fun calculateLayout(
        nodeIds: List<String>,
        edges: List<Edge>
    ): Map<String, Offset> {
        if (nodeIds.isEmpty()) return emptyMap()
        
        // Initialize nodes with random positions
        val nodes = nodeIds.map { id ->
            Node(
                id = id,
                x = Random.nextFloat() * width,
                y = Random.nextFloat() * height
            )
        }.associateBy { it.id }.toMutableMap()
        
        // Run force-directed iterations
        var temperature = initialTemp
        val coolingRate = initialTemp / iterations
        
        repeat(iterations) {
            // Reset forces
            nodes.values.forEach { node ->
                node.dx = 0f
                node.dy = 0f
            }
            
            // Calculate repulsive forces between all nodes
            nodes.values.forEach { v ->
                nodes.values.forEach { u ->
                    if (v.id != u.id) {
                        val dx = v.x - u.x
                        val dy = v.y - u.y
                        val distance = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                        
                        // Repulsive force: fr(d) = k^2 / d
                        val force = (k * k) / distance
                        
                        v.dx += (dx / distance) * force
                        v.dy += (dy / distance) * force
                    }
                }
            }
            
            // Calculate attractive forces for connected nodes
            edges.forEach { edge ->
                val source = nodes[edge.sourceId]
                val target = nodes[edge.targetId]
                
                if (source != null && target != null) {
                    val dx = target.x - source.x
                    val dy = target.y - source.y
                    val distance = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                    
                    // Attractive force: fa(d) = d^2 / k
                    val force = (distance * distance) / k
                    
                    val fx = (dx / distance) * force
                    val fy = (dy / distance) * force
                    
                    source.dx += fx
                    source.dy += fy
                    target.dx -= fx
                    target.dy -= fy
                }
            }
            
            // Update positions with cooling
            nodes.values.forEach { node ->
                val dx = node.dx
                val dy = node.dy
                val displacement = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                
                // Limit movement by temperature
                val limitedDisplacement = displacement.coerceAtMost(temperature)
                
                node.x += (dx / displacement) * limitedDisplacement
                node.y += (dy / displacement) * limitedDisplacement
                
                // Keep within bounds
                node.x = node.x.coerceIn(50f, width - 50f)
                node.y = node.y.coerceIn(50f, height - 50f)
            }
            
            // Cool down
            temperature -= coolingRate
        }
        
        // Return normalized positions (0-1 range)
        return nodes.mapValues { (_, node) ->
            Offset(
                x = node.x / width,
                y = node.y / height
            )
        }
    }
}
