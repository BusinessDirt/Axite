package github.businessdirt.axite.vanadium

import github.businessdirt.axite.vanadium.assets.ShaderCompiler
import github.businessdirt.axite.vanadium.core.events.Event
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.renderer.SceneRenderer
import github.businessdirt.axite.vanadium.renderer.graph.ClearColorValue
import github.businessdirt.axite.vanadium.renderer.graph.RenderGraph
import github.businessdirt.axite.vanadium.renderer.graph.RenderResourceNames
import github.businessdirt.axite.vanadium.renderer.scene.Scene
import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer
import github.businessdirt.axite.vanadium.vulkan.pipeline.GraphicsPipeline
import github.businessdirt.axite.vanadium.vulkan.resources.ShaderModule
import kotlinx.coroutines.CoroutineScope
import org.lwjgl.util.shaderc.Shaderc
import org.lwjgl.vulkan.VK10.VK_FORMAT_D16_UNORM
import org.lwjgl.vulkan.VK10.VK_FORMAT_D32_SFLOAT
import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT
import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO
import org.lwjgl.vulkan.VK10.vkCmdBindPipeline
import org.lwjgl.vulkan.VK10.vkCmdDraw
import org.lwjgl.vulkan.VkClearValue
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo


class VanadiumSandbox : VanadiumAdapter {

    companion object {
        const val FRAGMENT_SHADER_FILE_GLSL: String = "src/sandbox/resources/shaders/scene_frag.glsl"
        const val FRAGMENT_SHADER_FILE_SPV: String = "$FRAGMENT_SHADER_FILE_GLSL.spv"
        const val VERTEX_SHADER_FILE_GLSL: String = "src/sandbox/resources/shaders/scene_vert.glsl"
        const val VERTEX_SHADER_FILE_SPV: String = "$VERTEX_SHADER_FILE_GLSL.spv"
        const val DEPTH_FORMAT = VK_FORMAT_D32_SFLOAT
    }

    private val scene: Scene = Scene()


    private var graphicsPipeline: GraphicsPipeline? = null

    override suspend fun initialize(scope: CoroutineScope) {
        ShaderCompiler.compileShaderIfChanged(VERTEX_SHADER_FILE_GLSL, Shaderc.shaderc_vertex_shader)
        ShaderCompiler.compileShaderIfChanged(FRAGMENT_SHADER_FILE_GLSL, Shaderc.shaderc_fragment_shader)

        val vertexShader = ShaderModule(Vanadium.context.device.handle, VK_SHADER_STAGE_VERTEX_BIT, VERTEX_SHADER_FILE_SPV)
        val fragmentShader = ShaderModule(Vanadium.context.device.handle, VK_SHADER_STAGE_FRAGMENT_BIT, FRAGMENT_SHADER_FILE_SPV)

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
        pass("MainScenePass") {
            writes(RenderResourceNames.BACK_BUFFER, RenderResourceNames.DEPTH_BUFFER)
            clearColor = ClearColorValue(0.4f, 0.6f, 0.9f, 1.0f)
            clearDepth = 1.0f

            pipeline { commandBuffer ->
                vkCmdBindPipeline(commandBuffer.handle, 0, graphicsPipeline!!.handle)
                vkCmdDraw(commandBuffer.handle, 3, 1, 0, 0)
            }
        }
    }

    override fun onEvent(event: Event) {

    }
}