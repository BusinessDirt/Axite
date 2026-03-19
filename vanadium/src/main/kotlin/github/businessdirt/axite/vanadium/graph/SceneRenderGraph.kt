package github.businessdirt.axite.vanadium.graph

import github.businessdirt.axite.vanadium.platform.vulkan.Context
import github.businessdirt.axite.vanadium.platform.vulkan.command.CommandBuffer
import github.businessdirt.axite.vanadium.utils.imageBarrier
import github.businessdirt.axite.vanadium.utils.memoryStack
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkClearValue
import org.lwjgl.vulkan.VkRect2D
import org.lwjgl.vulkan.VkRenderingAttachmentInfo
import org.lwjgl.vulkan.VkRenderingInfo

class SceneRenderGraph {

    private val clrValueColor: VkClearValue = VkClearValue.calloc().apply {
        color { c ->
            c.float32(0, 0.5f).float32(1, 0.7f).float32(2, 0.9f).float32(3, 1.0f)
        }
    }

    private val attInfoColor: List<VkRenderingAttachmentInfo.Buffer> = List(Context.swapChain.imageCount) { i ->
        VkRenderingAttachmentInfo.calloc(1).`sType$Default`()
            .imageView(Context.swapChain.imageViews[i].handle)
            .imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
            .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
            .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
            .clearValue(clrValueColor)
    }

    private val renderInfo: List<VkRenderingInfo> = List(Context.swapChain.imageCount) { i ->
        val renderArea = VkRect2D.calloc()
            .extent(Context.swapChain.swapChainExtent)
            .offset { it.set(0, 0) }

        VkRenderingInfo.calloc().`sType$Default`()
            .renderArea(renderArea)
            .layerCount(1)
            .pColorAttachments(attInfoColor[i])
    }

    fun render(cmdBuffer: CommandBuffer, imageIndex: Int) {
        val swapChainImage = Context.swapChain.imageViews[imageIndex].handle
        val cmdHandle = cmdBuffer.handle

        memoryStack { stack ->
            // Transition Image to Color Attachment Optimal
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

            // TODO: ** PUT DRAW CALLS HERE **

            // End Dynamic Rendering
            vkCmdEndRendering(cmdHandle)

            // Transition Image for Presentation
            stack.imageBarrier(
                cmdHandle, swapChainImage,
                VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
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
        attInfoColor.forEach { it.free() }
        clrValueColor.free()
    }
}