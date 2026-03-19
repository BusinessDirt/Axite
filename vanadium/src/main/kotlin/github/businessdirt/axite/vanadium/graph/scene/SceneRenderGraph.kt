package github.businessdirt.axite.vanadium.graph.scene

import github.businessdirt.axite.vanadium.VanadiumConfig
import github.businessdirt.axite.vanadium.assets.ShaderCompiler
import github.businessdirt.axite.vanadium.graph.ModelCache
import github.businessdirt.axite.vanadium.platform.vulkan.Context
import github.businessdirt.axite.vanadium.platform.vulkan.command.CommandBuffer
import github.businessdirt.axite.vanadium.platform.vulkan.pipeline.Pipeline
import github.businessdirt.axite.vanadium.platform.vulkan.pipeline.ShaderModule
import github.businessdirt.axite.vanadium.utils.imageBarrier
import github.businessdirt.axite.vanadium.utils.memoryStack
import org.lwjgl.util.shaderc.Shaderc
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*

class SceneRenderGraph(config: VanadiumConfig) {

    companion object {
        const val FRAGMENT_SHADER_FILE_GLSL: String = "src/sandbox/resources/shaders/scene_frag.glsl"
        const val FRAGMENT_SHADER_FILE_SPV: String = "$FRAGMENT_SHADER_FILE_GLSL.spv"
        const val VERTEX_SHADER_FILE_GLSL: String = "src/sandbox/resources/shaders/scene_vert.glsl"
        const val VERTEX_SHADER_FILE_SPV: String = "$VERTEX_SHADER_FILE_GLSL.spv"
    }

    private val clrValueColor: VkClearValue = VkClearValue.calloc().apply {
        color { c ->
            c.float32(0, 0.5f).float32(1, 0.7f).float32(2, 0.9f).float32(3, 1.0f)
        }
    }

    private val pipeline: Pipeline = Pipeline { stack ->
        if (config.recompileShaders) {
            ShaderCompiler.compileShaderIfChanged(VERTEX_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_vertex_shader)
            ShaderCompiler.compileShaderIfChanged(FRAGMENT_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_fragment_shader)
        }

        val vertShader = ShaderModule(VK_SHADER_STAGE_VERTEX_BIT, VERTEX_SHADER_FILE_SPV)
        val fragShader = ShaderModule(VK_SHADER_STAGE_FRAGMENT_BIT, FRAGMENT_SHADER_FILE_SPV)

        colorFormat = Context.surface.surfaceFormat.imageFormat
        vertexInputState = VertexDefinition.createInputState(stack)
        shaders(vertShader, fragShader)
    }

    private val attachmentInfoColor: List<VkRenderingAttachmentInfo.Buffer> = List(Context.swapChain.imageCount) { i ->
        VkRenderingAttachmentInfo.calloc(1).`sType$Default`()
            .imageView(Context.swapChain.imageViews[i].handle)
            .imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
            .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
            .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
            .clearValue(clrValueColor)
    }

    private val renderInfo: List<VkRenderingInfo> = List(Context.swapChain.imageCount) { i ->
        val renderArea = VkRect2D.calloc()
            .extent(Context.swapChain.extent)
            .offset { it.set(0, 0) }

        VkRenderingInfo.calloc().`sType$Default`()
            .renderArea(renderArea)
            .layerCount(1)
            .pColorAttachments(attachmentInfoColor[i])
    }

    fun render(cmdBuffer: CommandBuffer, modelCache: ModelCache, imageIndex: Int) {
        val swapChainImage = Context.swapChain.imageViews[imageIndex].imageHandle
        val cmdHandle = cmdBuffer.handle

        memoryStack { stack ->
            stack.imageBarrier(
                cmdHandle, swapChainImage,
                VK_IMAGE_LAYOUT_UNDEFINED,
                VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT,
                VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT,
                VK_ACCESS_2_NONE,
                VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT,
                VK_IMAGE_ASPECT_COLOR_BIT
            )

            // Begin Dynamic Rendering
            vkCmdBeginRendering(cmdHandle, renderInfo[imageIndex])

            vkCmdBindPipeline(cmdHandle, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)

            val extent = Context.swapChain.extent
            val width = extent.width()
            val height = extent.height()

            val viewport = VkViewport.calloc(1, stack)
                .x(0f)
                .y(height.toFloat())
                .height(-height.toFloat())
                .width(width.toFloat())
                .minDepth(0.0f)
                .maxDepth(1.0f)
            vkCmdSetViewport(cmdHandle, 0, viewport)

            val scissor = VkRect2D.calloc(1, stack)
                .extent { it.width(width).height(height) }
                .offset { it.x(0).y(0) }
            vkCmdSetScissor(cmdHandle, 0, scissor)

            val offsets = stack.longs(0L)
            val pBuffer = stack.mallocLong(1)

            modelCache.modelMap.values.forEach { model ->
                model.meshList.forEach { mesh ->

                    // Bind Vertex Buffer
                    pBuffer.put(0, mesh.vertexBuffer.handle)
                    vkCmdBindVertexBuffers(cmdHandle, 0, pBuffer, offsets)

                    // Bind Index Buffer
                    vkCmdBindIndexBuffer(cmdHandle, mesh.indexBuffer.handle, 0, VK_INDEX_TYPE_UINT32)

                    // Draw
                    vkCmdDrawIndexed(cmdHandle, mesh.indexCount, 1, 0, 0, 0)
                }
            }

            // End Dynamic Rendering
            vkCmdEndRendering(cmdHandle)

            stack.imageBarrier(
                cmdHandle, swapChainImage,
                VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
                VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT,
                VK_PIPELINE_STAGE_2_BOTTOM_OF_PIPE_BIT,
                VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT,
                VK_ACCESS_2_NONE,
                VK_IMAGE_ASPECT_COLOR_BIT
            )
        }
    }

    fun cleanup() {
        renderInfo.forEach {
            it.renderArea().free() // Free the inner rect we allocated
            it.free()
        }
        attachmentInfoColor.forEach { it.free() }
        clrValueColor.free()
    }
}