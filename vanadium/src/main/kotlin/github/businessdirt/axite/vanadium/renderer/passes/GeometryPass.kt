package github.businessdirt.axite.vanadium.renderer.passes

import github.businessdirt.axite.vanadium.renderer.graph.RenderGraph
import github.businessdirt.axite.vanadium.renderer.graph.RenderPassNode
import github.businessdirt.axite.vanadium.renderer.scene.Scene

object GeometryPass {

    /**
     * Configures and adds a geometry pass to the provided RenderGraph.
     */
    fun addToGraph(graph: RenderGraph, scene: Scene): RenderPassNode = graph.addPass(
        name = "BaseGeometryPass",
        reads = emptySet(), // Usually reads nothing, or maybe ShadowMaps
        writes = setOf("MainColorBuffer", "MainDepthBuffer"),
        dependencies = emptyList() // Usually runs early in the frame
    ) {
        // TODO: VULKAN COMMANDS GO HERE:
        // 1. Bind Pipeline
        // 2. Bind Descriptor Sets (Global Uniforms)
        // 3. Loop through scene.entities and issue vkCmdDrawIndexed
        println("Executing Geometry Pass: Drawing ${scene.entities.size} entities.")
    }
}