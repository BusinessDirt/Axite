package github.businessdirt.axite.vanadium.vulkan.commands

import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.resources.Attachment
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
import org.lwjgl.vulkan.VK13.*

fun CommandBuffer.beginRendering(
    width: Int, height: Int,
    colorAttachments: List<Attachment>,
    depthAttachment: Attachment?,
    clearValueColor: VkClearValue? = null,
    clearValueDepth: VkClearValue? = null,
) = memoryStack { stack ->
    val pColorAttachments = VkRenderingAttachmentInfo.calloc(colorAttachments.size, stack)
    colorAttachments.forEachIndexed { index, attachment ->
        val loadOp = if (clearValueColor != null) VK_ATTACHMENT_LOAD_OP_CLEAR else VK_ATTACHMENT_LOAD_OP_LOAD
        pColorAttachments[index].`sType$Default`()
            .imageView(attachment.imageView.handle)
            .imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
            .loadOp(loadOp)
            .storeOp(VK_ATTACHMENT_STORE_OP_STORE)

        if (loadOp == VK_ATTACHMENT_LOAD_OP_CLEAR) pColorAttachments[index].clearValue(clearValueColor!!)
    }

    val renderInfo = VkRenderingInfo.calloc(stack).`sType$Default`()
        .renderArea { it.extent { e -> e.set(width, height) } }
        .layerCount(1)
        .pColorAttachments(pColorAttachments)

    if (depthAttachment != null) {
        val loadOp = if (clearValueDepth != null) VK_ATTACHMENT_LOAD_OP_CLEAR else VK_ATTACHMENT_LOAD_OP_LOAD
        val pDepthAttachment = VkRenderingAttachmentInfo.calloc(stack).`sType$Default`()
            .imageView(depthAttachment.imageView.handle)
            .imageLayout(VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL)
            .loadOp(loadOp)
            .storeOp(VK_ATTACHMENT_STORE_OP_STORE)

        if (loadOp == VK_ATTACHMENT_LOAD_OP_CLEAR) pDepthAttachment.clearValue(clearValueDepth!!)
        renderInfo.pDepthAttachment(pDepthAttachment)
    }

    vkCmdBeginRendering(this.handle, renderInfo)
}

fun CommandBuffer.setViewport(width: Float, height: Float, x: Float = 0f, y: Float = 0f) = memoryStack { stack ->
    val viewport = VkViewport.calloc(1, stack)
        .x(x)
        .y(y)
        .width(width)
        .height(height)
        .minDepth(0.0f)
        .maxDepth(1.0f)

    vkCmdSetViewport(this.handle, 0, viewport)
}

fun CommandBuffer.setScissor(width: Int, height: Int, offsetX: Int = 0, offsetY: Int = 0) = memoryStack { stack ->
    val scissor = VkRect2D.calloc(1, stack)
    scissor.offset { it.set(offsetX, offsetY) }
    scissor.extent { it.set(width, height) }

    vkCmdSetScissor(this.handle, 0, scissor)
}

fun CommandBuffer.endRendering() = vkCmdEndRendering(this.handle)

fun CommandBuffer.transitionLayout(
    attachment: Attachment,
    newLayout: Int
) {
    if (attachment.currentLayout == newLayout) return

    memoryStack { stack ->
        val barrier = VkImageMemoryBarrier2.calloc(1, stack).`sType$Default`()
            .srcStageMask(getStageMask(attachment.currentLayout))
            .srcAccessMask(getAccessMask(attachment.currentLayout))
            .dstStageMask(getStageMask(newLayout))
            .dstAccessMask(getAccessMask(newLayout))
            .oldLayout(attachment.currentLayout)
            .newLayout(newLayout)
            .image(attachment.image.handle)
            .subresourceRange {
                it.aspectMask(attachment.aspectMask)
                    .baseMipLevel(0)
                    .levelCount(VK_REMAINING_MIP_LEVELS)
                    .baseArrayLayer(0)
                    .layerCount(VK_REMAINING_ARRAY_LAYERS)
            }

        val dependencyInfo = VkDependencyInfo.calloc(stack).`sType$Default`()
            .pImageMemoryBarriers(barrier)

        vkCmdPipelineBarrier2(this.handle, dependencyInfo)
    }

    attachment.currentLayout = newLayout
}

private fun getAccessMask(layout: Int): Long = when (layout) {
    VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL -> VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT
    VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL -> VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT
    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL -> VK_ACCESS_2_SHADER_READ_BIT
    VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL -> VK_ACCESS_2_TRANSFER_READ_BIT
    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL -> VK_ACCESS_2_TRANSFER_WRITE_BIT
    VK_IMAGE_LAYOUT_PRESENT_SRC_KHR -> 0L
    else -> 0L
}

private fun getStageMask(layout: Int): Long = when (layout) {
    VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL -> VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT
    VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL -> VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT or VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT
    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL -> VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT
    VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL -> VK_PIPELINE_STAGE_2_TRANSFER_BIT
    VK_IMAGE_LAYOUT_PRESENT_SRC_KHR -> VK_PIPELINE_STAGE_2_BOTTOM_OF_PIPE_BIT
    else -> VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT
}