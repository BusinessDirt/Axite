package github.businessdirt.axite.vanadium

import github.businessdirt.axite.vanadium.core.events.Event
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.renderer.SceneRenderer
import github.businessdirt.axite.vanadium.renderer.graph.ClearColorValue
import github.businessdirt.axite.vanadium.renderer.graph.RenderGraph
import github.businessdirt.axite.vanadium.renderer.graph.RenderResourceNames
import github.businessdirt.axite.vanadium.renderer.scene.Scene
import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer
import kotlinx.coroutines.CoroutineScope
import org.lwjgl.vulkan.VkClearValue


class VanadiumSandbox : VanadiumAdapter {

    private val scene: Scene = Scene()

    override suspend fun initialize(scope: CoroutineScope) {

    }

    override fun update(frameInfo: FrameInfo) {

    }

    override fun onRecord(graph: RenderGraph, sceneRenderer: SceneRenderer, commandBuffer: CommandBuffer, interpolation: Double) {
        graph.addPass(
            name = "MainScenePass",
            writes = setOf(RenderResourceNames.BACK_BUFFER, RenderResourceNames.DEPTH_BUFFER),
            clearColor = ClearColorValue(0.4f, 0.6f, 0.9f, 1.0f),
            clearDepth = 1.0f
        ) {
            sceneRenderer.drawScene(scene, it, interpolation)
        }
    }

    override fun onEvent(event: Event) {

    }
}