package github.businessdirt.axite.vanadium.renderer.graph

import github.businessdirt.axite.vanadium.core.dag.DirectedAcyclicGraph
import github.businessdirt.axite.vanadium.vulkan.Context
import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer
import github.businessdirt.axite.vanadium.vulkan.resources.Attachment
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkClearValue
import org.lwjgl.vulkan.VkRenderingAttachmentInfo
import org.lwjgl.vulkan.VkRenderingInfo

class RenderGraph(
    private val context: Context
) : DirectedAcyclicGraph<RenderPassData>() {

    private val registry = ResourceRegistry(context)

    /**
     * Creates a RenderPassNode, sets up dependencies, and registers it to the graph.
     */
    fun addPass(
        name: String,
        reads: Set<String> = emptySet(),
        writes: Set<String> = emptySet(),
        dependencies: List<RenderPassNode> = emptyList(),
        action: () -> Unit
    ): RenderPassNode = RenderPassNode(name, reads, writes, action).apply {
        this.dependencies.addAll(dependencies)
        nodes.add(this)
    }

    fun execute(commandBuffer: CommandBuffer) {

    }

    fun use(commandBuffer: CommandBuffer, action: (RenderGraph) -> Unit) {
        nodes.clear()
        layers.clear()
        resourceLifetimes.clear()

        action(this)

        compile()
        execute(commandBuffer)
    }
}

fun CommandBuffer.beginRendering(
    width: Int, height: Int,
    colorAttachment: Attachment,
    depthAttachment: Attachment,
    clearValueColor: VkClearValue,
    clearValueDepth: VkClearValue,
) {
    val colorAttachment = VkRenderingAttachmentInfo.calloc(1).`sType$Default`()
        .imageView(colorAttachment.imageView.handle)
        .imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
        .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
        .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
        .clearValue(clearValueColor)

    val depthAttachment = VkRenderingAttachmentInfo.calloc().`sType$Default`()
        .imageView(depthAttachment.imageView.handle)
        .imageLayout(VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL)
        .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
        .storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
        .clearValue(clearValueDepth)

    val renderInfo = VkRenderingInfo.calloc().`sType$Default`()
        .renderArea { it.extent { e -> e.set(width, height) } }
        .layerCount(1)
        .pColorAttachments(colorAttachment)
        .pDepthAttachment(depthAttachment)

    vkCmdBeginRendering(this.handle, renderInfo)
}

fun CommandBuffer.endRendering() = vkCmdEndRendering(this.handle)