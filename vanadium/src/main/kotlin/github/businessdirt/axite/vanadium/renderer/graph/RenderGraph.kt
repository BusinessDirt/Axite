package github.businessdirt.axite.vanadium.renderer.graph

import github.businessdirt.axite.vanadium.core.dag.DirectedAcyclicGraph
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.Context
import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer
import github.businessdirt.axite.vanadium.vulkan.resources.Attachment
import github.businessdirt.axite.vanadium.vulkan.resources.Image
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
import org.lwjgl.vulkan.VK13.*

class RenderGraph(
    private val context: Context
) : DirectedAcyclicGraph<RenderPassData>() {

    val registry = ResourceRegistry(context)

    /**
     * Creates a RenderPassNode, sets up dependencies, and registers it to the graph.
     */
    fun addPass(
        name: String,
        reads: Set<String> = emptySet(),
        writes: Set<String> = emptySet(),
        dependencies: List<RenderPassNode> = emptyList(),
        clearColor: VkClearValue? = null,
        clearDepth: VkClearValue? = null,
        action: (CommandBuffer) -> Unit
    ): RenderPassNode = RenderPassNode(name, reads, writes, action).apply {
        this.data.clearColor = clearColor
        this.data.clearDepth = clearDepth
        this.dependencies.addAll(dependencies)
        nodes.add(this)
    }

    fun importResource(name: String, image: Image, width: Int, height: Int, format: Int, usage: Int) {
        registry.importResource(name, image, width, height, format, usage)
    }

    fun createResource(name: String, width: Int, height: Int, format: Int, usage: Int) {
        registry.createResource(name, width, height, format, usage)
    }

    fun execute(commandBuffer: CommandBuffer) {
        sortedNodes.forEach { node ->
            val passNode = node as RenderPassNode

            // Transition resources
            passNode.writeResources.forEach { res ->
                val attachment = registry.get(res)
                val targetLayout = if (attachment.isDepth) VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL else VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL
                commandBuffer.transitionLayout(attachment, targetLayout)
            }

            passNode.readResources.forEach { res ->
                val attachment = registry.get(res)
                commandBuffer.transitionLayout(attachment, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
            }

            // Begin rendering
            val colorAttachments = passNode.writeResources.map { registry.get(it) }.filter { !it.isDepth }
            val depthAttachment = passNode.writeResources.map { registry.get(it) }.find { it.isDepth }

            if (colorAttachments.isNotEmpty() || depthAttachment != null) {
                val width = (colorAttachments.firstOrNull() ?: depthAttachment!!).width
                val height = (colorAttachments.firstOrNull() ?: depthAttachment!!).height

                commandBuffer.beginRendering(
                    width, height,
                    colorAttachments,
                    depthAttachment,
                    passNode.data.clearColor,
                    passNode.data.clearDepth
                )
            }

            // Execute pass
            passNode.execute(commandBuffer)

            // End rendering
            if (colorAttachments.isNotEmpty() || depthAttachment != null) commandBuffer.endRendering()
        }
    }

    fun use(commandBuffer: CommandBuffer, action: (RenderGraph) -> Unit) {
        nodes.clear()
        layers.clear()
        resourceLifetimes.clear()

        action(this)

        compile()
        execute(commandBuffer)
    }

    fun clear() {
        registry.clear()
    }
}

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
        
        if (loadOp == VK_ATTACHMENT_LOAD_OP_CLEAR && clearValueColor != null) {
            pColorAttachments[index].clearValue(clearValueColor)
        }
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

        if (loadOp == VK_ATTACHMENT_LOAD_OP_CLEAR && clearValueDepth != null) {
            pDepthAttachment.clearValue(clearValueDepth)
        }
        renderInfo.pDepthAttachment(pDepthAttachment)
    }

    vkCmdBeginRendering(this.handle, renderInfo)
}

fun CommandBuffer.endRendering() = vkCmdEndRendering(this.handle)