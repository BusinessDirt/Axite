package github.businessdirt.axite.vanadium.renderer.graph

import github.businessdirt.axite.vanadium.core.dag.DirectedAcyclicGraph
import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer

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

    fun use(commandBuffer: CommandBuffer, action: (RenderGraph) -> Unit) {
        nodes.clear()
        layers.clear()
        resourceLifetimes.clear()

        action(this)

        compile()
        execute()
    }
}