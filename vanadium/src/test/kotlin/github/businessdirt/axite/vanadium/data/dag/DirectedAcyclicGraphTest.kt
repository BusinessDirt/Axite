package github.businessdirt.axite.vanadium.data.dag

import github.businessdirt.axite.vanadium.core.dag.DirectedAcyclicGraph
import github.businessdirt.axite.vanadium.core.dag.Node
import github.businessdirt.axite.vanadium.core.dag.ResourceUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test

/**
 * A simple test implementation of [github.businessdirt.axite.vanadium.core.dag.Node].
 */
class TestNode(name: String) : Node<String>(name) {
    override fun execute() {
        println("Executing $data")
    }
}

/**
 * A test implementation of [Node] that also implements [github.businessdirt.axite.vanadium.core.dag.ResourceUser].
 */
class ResourceNode(
    name: String,
    override val readResources: Set<String> = emptySet(),
    override val writeResources: Set<String> = emptySet()
) : Node<String>(name), ResourceUser {
    override fun execute() {}
}

/**
 * Tests for [github.businessdirt.axite.vanadium.core.dag.DirectedAcyclicGraph].
 */
class DirectedAcyclicGraphTest {

    @Test
    @DisplayName("topologicalSort should order dependencies correctly")
    fun `topologicalSort should order dependencies correctly`() {
        val dag = DirectedAcyclicGraph<String>()

        val a = TestNode("A")
        val b = TestNode("B")
        val c = TestNode("C")

        // C depends on B, B depends on A (A -> B -> C)
        c.dependencies.add(b)
        b.dependencies.add(a)

        dag.nodes.addAll(listOf(a, b, c))
        dag.compile()
        val sorted = dag.sortedNodes

        assertEquals(listOf(a, b, c), sorted, "The order must be A, B, C.")
    }

    @Test
    @DisplayName("topologicalSort should throw exception on cycle")
    fun `topologicalSort should throw exception on cycle`() {
        val dag = DirectedAcyclicGraph<String>()

        val a = TestNode("A")
        val b = TestNode("B")

        // Cycle: A -> B and B -> A
        a.dependencies.add(b)
        b.dependencies.add(a)

        dag.nodes.addAll(listOf(a, b))

        assertThrows<IllegalStateException> {
            dag.compile()
        }
    }

    @Test
    @DisplayName("calculateBarriers should group independent nodes into layers")
    fun `calculateBarriers should group independent nodes into layers`() {
        val dag = DirectedAcyclicGraph<String>()

        val start = TestNode("Start")
        val p1 = TestNode("Parallel 1")
        val p2 = TestNode("Parallel 2")
        val end = TestNode("End")

        // Structure:
        // Start -> P1 -> End
        // Start -> P2 -> End
        p1.dependencies.add(start)
        p2.dependencies.add(start)
        end.dependencies.add(p1)
        end.dependencies.add(p2)

        dag.nodes.addAll(listOf(start, p1, p2, end))
        dag.compile()

        // We expect 3 layers:
        // Layer 0: [Start]
        // Layer 1: [P1, P2] (order doesn't matter)
        // Layer 2: [End]

        assertEquals(3, dag.layers.size)
        assertTrue(dag.layers[0].contains(start))
        assertTrue(dag.layers[1].containsAll(listOf(p1, p2)))
        assertTrue(dag.layers[2].contains(end))
    }

    @Test
    @DisplayName("lifetimeAnalysis should detect first and last usage of resources")
    fun `lifetimeAnalysis should detect first and last usage of resources`() {
        val dag = DirectedAcyclicGraph<String>()

        val node0 = ResourceNode("Node 0", writeResources = setOf("BufferA"))
        val node1 = ResourceNode("Node 1", readResources = setOf("BufferA"))
        val node2 = ResourceNode("Node 2", readResources = setOf("BufferA"))
        val node3 = ResourceNode("Node 3", writeResources = setOf("BufferB"))

        // Sequential: 0 -> 1 -> 2 -> 3
        node1.dependencies.add(node0)
        node2.dependencies.add(node1)
        node3.dependencies.add(node2)

        dag.nodes.addAll(listOf(node0, node1, node2, node3))
        dag.compile()

        // BufferA is created in Node 0 and last read in Node 2.
        // Lifetime should be 0..2.
        // BufferB is only used in Node 3 (3..3).

        val lifetimes = dag.resourceLifetimes
        assertEquals(0..2, lifetimes["BufferA"])
        assertEquals(3..3, lifetimes["BufferB"])
    }
}
