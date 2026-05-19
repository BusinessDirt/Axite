package github.businessdirt.axite.vanadium.vulkan.commands

import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.renderer.graph.AttachmentRenderSettings
import github.businessdirt.axite.vanadium.vulkan.resources.Attachment
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
import org.lwjgl.vulkan.VK13.*

fun CommandBuffer.beginRendering(
    width: Int, height: Int,
    colorAttachments: List<AttachmentRenderSettings>,
    depthSettings: AttachmentRenderSettings?,
    clearValueColor: VkClearValue? = null,
    clearValueDepth: VkClearValue? = null,
) = memoryStack { stack ->
    // Setup Color Attachments
    val pColorAttachments = VkRenderingAttachmentInfo.calloc(colorAttachments.size, stack)
    colorAttachments.forEachIndexed { i, settings ->
        pColorAttachments[i].`sType$Default`()
            .imageView(settings.attachment.imageView.handle)
            .imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
            .loadOp(settings.loadOp)
            .storeOp(settings.storeOp)

        if (settings.loadOp == VK_ATTACHMENT_LOAD_OP_CLEAR && clearValueColor != null) {
            pColorAttachments[i].clearValue(clearValueColor)
        }
    }

    // Base Rendering Info
    val renderInfo = VkRenderingInfo.calloc(stack).`sType$Default`()
        .renderArea { it.extent { e -> e.set(width, height) } }
        .layerCount(1)
        .pColorAttachments(pColorAttachments)

    // Setup Depth Attachment
    if (depthSettings != null) {
        val pDepthAttachment = VkRenderingAttachmentInfo.calloc(stack).`sType$Default`()
            .imageView(depthSettings.attachment.imageView.handle)
            .imageLayout(VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL)
            .loadOp(depthSettings.loadOp)
            .storeOp(depthSettings.storeOp)

        // Only clear if the Op says so AND we have a value
        if (depthSettings.loadOp == VK_ATTACHMENT_LOAD_OP_CLEAR && clearValueDepth != null)
            pDepthAttachment.clearValue(clearValueDepth)

        renderInfo.pDepthAttachment(pDepthAttachment)
    }

    vkCmdBeginRendering(this.handle, renderInfo)
}

fun CommandBuffer.setViewport(width: Float, height: Float, x: Float = 0f, y: Float = 0f) = memoryStack { stack ->
    val viewport = VkViewport.calloc(1, stack)
        .x(x).y(y)
        .width(width).height(height)
        .minDepth(0.0f).maxDepth(1.0f)

    vkCmdSetViewport(this.handle, 0, viewport)
}

fun CommandBuffer.setScissor(width: Int, height: Int, offsetX: Int = 0, offsetY: Int = 0) = memoryStack { stack ->
    val scissor = VkRect2D.calloc(1, stack)
        .offset { it.set(offsetX, offsetY) }
        .extent { it.set(width, height) }

    vkCmdSetScissor(this.handle, 0, scissor)
}

fun CommandBuffer.endRendering() = vkCmdEndRendering(this.handle)

fun CommandBuffer.transitionLayout(
    attachment: Attachment,
    newLayout: Int
) {
    // Optimization: Skip if already in layout
    if (attachment.currentLayout == newLayout && newLayout != VK_IMAGE_LAYOUT_UNDEFINED) return

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
    VK_IMAGE_LAYOUT_UNDEFINED -> 0L
    VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL -> VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT
    VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL -> VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_READ_BIT or VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT
    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL -> VK_ACCESS_2_SHADER_READ_BIT
    VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL -> VK_ACCESS_2_TRANSFER_READ_BIT
    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL -> VK_ACCESS_2_TRANSFER_WRITE_BIT
    else -> 0L
}

private fun getStageMask(layout: Int): Long = when (layout) {
    VK_IMAGE_LAYOUT_UNDEFINED -> VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT
    VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL -> VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT
    VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL -> VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT or VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT
    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL -> VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT
    VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL -> VK_PIPELINE_STAGE_2_TRANSFER_BIT
    VK_IMAGE_LAYOUT_PRESENT_SRC_KHR -> VK_PIPELINE_STAGE_2_BOTTOM_OF_PIPE_BIT
    else -> VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT
}

fun CommandBuffer.draw(
    vertexCount: Int,
    instanceCount: Int = 1,
    firstVertex: Int = 0,
    firstInstance: Int = 0,
) = vkCmdDraw(handle, vertexCount, instanceCount, firstVertex, firstInstance)

fun CommandBuffer.drawIndexed(
    indexCount: Int,
    instanceCount: Int = 1,
    firstIndex: Int = 0,
    vertexOffset: Int = 0,
    firstInstance: Int = 0,
) = vkCmdDrawIndexed(handle, indexCount, instanceCount, firstIndex, vertexOffset, firstInstance)

fun CommandBuffer.bindVertexBuffer(buffer: Long, offset: Long = 0) = memoryStack { stack ->
    val pBuffers = stack.longs(buffer)
    val pOffsets = stack.longs(offset)
    vkCmdBindVertexBuffers(handle, 0, pBuffers, pOffsets)
}

fun CommandBuffer.bindIndexBuffer(buffer: Long, offset: Long = 0, indexType: Int = VK_INDEX_TYPE_UINT32) {
    vkCmdBindIndexBuffer(handle, buffer, offset, indexType)
}

fun CommandBuffer.pushConstants(layout: Long, stageFlags: Int, data: java.nio.ByteBuffer, offset: Int = 0) {
    vkCmdPushConstants(handle, layout, stageFlags, offset, data)
}

fun CommandBuffer.bindDescriptorSets(pipelineLayout: Long, descriptorSets: LongArray, bindPoint: Int = VK_PIPELINE_BIND_POINT_GRAPHICS, firstSet: Int = 0) = memoryStack { stack ->
    vkCmdBindDescriptorSets(handle, bindPoint, pipelineLayout, firstSet, stack.longs(*descriptorSets), null)
}