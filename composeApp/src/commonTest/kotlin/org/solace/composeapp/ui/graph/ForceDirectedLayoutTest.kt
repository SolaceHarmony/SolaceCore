package org.solace.composeapp.ui.graph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for ForceDirectedLayout algorithm
 */
class ForceDirectedLayoutTest {
    
    @Test
    fun testEmptyLayout() {
        val layout = ForceDirectedLayout()
        val result = layout.calculateLayout(emptyList(), emptyList())
        
        assertTrue(result.isEmpty(), "Empty input should produce empty output")
    }
    
    @Test
    fun testSingleNode() {
        val layout = ForceDirectedLayout()
        val nodes = listOf("node1")
        val result = layout.calculateLayout(nodes, emptyList())
        
        assertEquals(1, result.size, "Should have one node")
        assertTrue(result.containsKey("node1"), "Should contain node1")
        
        val position = result["node1"]!!
        assertTrue(position.x in 0f..1f, "X position should be normalized")
        assertTrue(position.y in 0f..1f, "Y position should be normalized")
    }
    
    @Test
    fun testTwoNodesWithEdge() {
        val layout = ForceDirectedLayout()
        val nodes = listOf("node1", "node2")
        val edges = listOf(ForceDirectedLayout.Edge("node1", "node2"))
        val result = layout.calculateLayout(nodes, edges)
        
        assertEquals(2, result.size, "Should have two nodes")
        assertTrue(result.containsKey("node1"), "Should contain node1")
        assertTrue(result.containsKey("node2"), "Should contain node2")
        
        // Verify positions are normalized
        result.values.forEach { position ->
            assertTrue(position.x in 0f..1f, "X position should be normalized")
            assertTrue(position.y in 0f..1f, "Y position should be normalized")
        }
    }
    
    @Test
    fun testMultipleNodes() {
        val layout = ForceDirectedLayout()
        val nodes = listOf("node1", "node2", "node3", "node4")
        val edges = listOf(
            ForceDirectedLayout.Edge("node1", "node2"),
            ForceDirectedLayout.Edge("node2", "node3"),
            ForceDirectedLayout.Edge("node3", "node4")
        )
        val result = layout.calculateLayout(nodes, edges)
        
        assertEquals(4, result.size, "Should have four nodes")
        
        // All nodes should be present
        nodes.forEach { nodeId ->
            assertTrue(result.containsKey(nodeId), "Should contain $nodeId")
        }
        
        // All positions should be normalized
        result.values.forEach { position ->
            assertTrue(position.x in 0f..1f, "X position should be normalized: ${position.x}")
            assertTrue(position.y in 0f..1f, "Y position should be normalized: ${position.y}")
        }
    }
    
    @Test
    fun testDisconnectedNodes() {
        val layout = ForceDirectedLayout()
        val nodes = listOf("node1", "node2", "node3")
        // No edges - nodes should repel each other
        val result = layout.calculateLayout(nodes, emptyList())
        
        assertEquals(3, result.size, "Should have three nodes")
        
        // Verify all positions are different (nodes repelled each other)
        val positions = result.values.toList()
        for (i in 0 until positions.size) {
            for (j in i + 1 until positions.size) {
                val pos1 = positions[i]
                val pos2 = positions[j]
                // They should be at different positions (not on top of each other)
                val distance = kotlin.math.sqrt(
                    (pos1.x - pos2.x) * (pos1.x - pos2.x) + 
                    (pos1.y - pos2.y) * (pos1.y - pos2.y)
                )
                assertTrue(distance > 0.01f, "Nodes should be separated by repulsion")
            }
        }
    }
}
