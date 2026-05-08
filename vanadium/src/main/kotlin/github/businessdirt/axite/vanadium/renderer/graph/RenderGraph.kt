package github.businessdirt.axite.vanadium.renderer.graph

import github.businessdirt.axite.vanadium.core.dag.DirectedAcyclicGraph

class RenderGraph : DirectedAcyclicGraph<RenderPassData>() {

    /**
     * Creates a RenderPassNode, sets up dependencies, and registers it to the graph.
     */
    fun addPass(
        name: String,
        reads: Set<String> = emptySet(),
        writes: Set<String> = emptySet(),
        dependencies: List<RenderPassNode> = emptyList(),
        action: () -> Unit
    ): RenderPassNode = RenderPassNode(name, reads, writes, action).apply {
        this.dependencies.addAll(dependencies)
        nodes.add(this)
    }

    /**
     * Clears the graph structure and all compiled metadata.
     * Use this if you need to rebuild the graph every frame.
     */
    fun reset() {
        nodes.clear()
        layers.clear()
        resourceLifetimes.clear()
    }
}