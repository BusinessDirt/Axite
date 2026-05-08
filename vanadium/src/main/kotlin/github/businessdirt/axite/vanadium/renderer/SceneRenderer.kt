package github.businessdirt.axite.vanadium.renderer

import github.businessdirt.axite.vanadium.renderer.graph.RenderGraph
import github.businessdirt.axite.vanadium.renderer.passes.GeometryPass
import github.businessdirt.axite.vanadium.renderer.scene.Scene
import github.businessdirt.axite.vanadium.vulkan.Context

class SceneRenderer(val context: Context) {

    /**
     * This is called every frame to "record" the scene's intentions
     * into the RenderGraph.
     */
    fun recordPasses(graph: RenderGraph, scene: Scene) {
        // TODO: Add a Shadow Pass here first
        // val shadowNode = ShadowPass.addToGraph(graph, scene)

        // Add the main geometry pass
        val geometryNode = GeometryPass.addToGraph(graph, scene)

        // TODO: In the future, pass 'geometryNode' as a dependency
        // to a PostProcessPass (e.g., Bloom or ToneMapping)
    }

    fun cleanup() {
        // Clean up Vulkan resources tied specifically to scene rendering
        // (Pipelines, Descriptor Set Layouts, etc.)
    }
}