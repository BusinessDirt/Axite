package github.businessdirt.axite.vanadium.renderer

import github.businessdirt.axite.vanadium.renderer.graph.RenderGraph
import github.businessdirt.axite.vanadium.renderer.scene.Scene
import github.businessdirt.axite.vanadium.vulkan.Context

class Renderer(val context: Context) {

    private val renderGraph = RenderGraph()
    private val sceneRenderer = SceneRenderer(context)

    fun render(scene: Scene) {
        // TODO: Wait for GPU (Fences)
        // TODO: Acquire Image from Swapchain

        // Record/Build the frame
        renderGraph.reset()

        // SceneRenderer adds its passes to the graph
        // TODO: sceneRenderer.recordPasses(renderGraph, scene)

        // Add a UI pass maybe?

        // Execute the Graph
        renderGraph.compile()
        renderGraph.execute()

        // TODO: Present the image
    }
}