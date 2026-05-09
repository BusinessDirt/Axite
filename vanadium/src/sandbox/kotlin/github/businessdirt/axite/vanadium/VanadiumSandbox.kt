package github.businessdirt.axite.vanadium

import github.businessdirt.axite.vanadium.core.events.Event
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.renderer.SceneRenderer
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
        memoryStack { stack ->
            val clearColor = VkClearValue.calloc(stack)
            clearColor.color().float32(0, 0.4f).float32(1, 0.6f).float32(2, 0.9f).float32(3, 1.0f)

            val clearDepth = VkClearValue.calloc(stack)
            clearDepth.depthStencil().depth(1.0f)

            graph.addPass(
                name = "MainScenePass",
                writes = setOf(RenderResourceNames.BACK_BUFFER, RenderResourceNames.DEPTH_BUFFER),
                clearColor = clearColor,
                clearDepth = clearDepth
            ) {
                sceneRenderer.drawScene(scene, it, interpolation)
            }
        }
    }

    override fun onEvent(event: Event) {

    }
}