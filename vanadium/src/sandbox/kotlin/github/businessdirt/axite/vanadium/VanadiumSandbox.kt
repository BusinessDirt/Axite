package github.businessdirt.axite.vanadium

import github.businessdirt.axite.vanadium.assets.types.Shader
import github.businessdirt.axite.vanadium.core.events.Event
import github.businessdirt.axite.vanadium.renderer.SceneRenderer
import github.businessdirt.axite.vanadium.renderer.graph.ClearColorValue
import github.businessdirt.axite.vanadium.renderer.graph.RenderGraph
import github.businessdirt.axite.vanadium.renderer.graph.RenderResourceNames
import github.businessdirt.axite.vanadium.renderer.scene.Scene
import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer
import github.businessdirt.axite.vanadium.vulkan.commands.setScissor
import github.businessdirt.axite.vanadium.vulkan.commands.setViewport
import github.businessdirt.axite.vanadium.vulkan.pipeline.GraphicsPipeline
import kotlinx.coroutines.CoroutineScope
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo


class VanadiumSandbox : VanadiumAdapter {

    companion object {
        const val FRAGMENT_SHADER_FILE_GLSL: String = "src/sandbox/resources/shaders/scene.frag.glsl"
        const val VERTEX_SHADER_FILE_GLSL: String = "src/sandbox/resources/shaders/scene.vert.glsl"
        const val DEPTH_FORMAT = VK_FORMAT_D32_SFLOAT
    }

    private val scene: Scene = Scene()

    private var graphicsPipeline: GraphicsPipeline? = null

    override suspend fun initialize(scope: CoroutineScope) {
        val vertexShader = Vanadium.assets.load<Shader>(VERTEX_SHADER_FILE_GLSL)
        val fragmentShader = Vanadium.assets.load<Shader>(FRAGMENT_SHADER_FILE_GLSL)

        graphicsPipeline = GraphicsPipeline.create(Vanadium.context.device.handle, Vanadium.context.pipelineCache.handle) {
            shaders(vertexShader, fragmentShader)

            val emptyVertexInputState = VkPipelineVertexInputStateCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                .pVertexBindingDescriptions(null)
                .pVertexAttributeDescriptions(null)

            vertexInput(emptyVertexInputState)

            colorFormat = Vanadium.context.surface.surfaceFormat.imageFormat
            depthFormat = DEPTH_FORMAT
        }

        vertexShader.close()
        fragmentShader.close()
    }

    override fun shutdown() {
        graphicsPipeline?.close()
        graphicsPipeline = null
    }

    override fun update(frameInfo: FrameInfo) {

    }

    override fun onRecord(graph: RenderGraph, sceneRenderer: SceneRenderer, commandBuffer: CommandBuffer, interpolation: Double) = graph.build {
        val fbWidth = Vanadium.context.swapchain.extent.width()
        val fbHeight = Vanadium.context.swapchain.extent.height()

        pass("MainScenePass") {
            writes(RenderResourceNames.BACK_BUFFER, RenderResourceNames.DEPTH_BUFFER)
            clearColor = ClearColorValue(0.4f, 0.6f, 0.9f, 1.0f)
            clearDepth = 1.0f

            pipeline { commandBuffer ->
                vkCmdBindPipeline(commandBuffer.handle, 0, graphicsPipeline!!.handle)
                vkCmdDraw(commandBuffer.handle, 3, 1, 0, 0)
            }
        }

        pass("DebugScenePass") {
            writes(RenderResourceNames.BACK_BUFFER, RenderResourceNames.DEPTH_BUFFER)
            pipeline { commandBuffer ->
                commandBuffer.setScissor(fbWidth / 2, fbHeight / 2, fbWidth / 2, fbHeight / 2)
                commandBuffer.setViewport((fbWidth / 2).toFloat(), (fbHeight / 2).toFloat(),
                    (fbWidth / 2).toFloat(), (fbHeight / 2).toFloat()
                )
                vkCmdBindPipeline(commandBuffer.handle, 0, graphicsPipeline!!.handle)
                vkCmdDraw(commandBuffer.handle, 3, 1, 0, 0)
            }
        }
    }

    override fun onEvent(event: Event) {

    }
}