package github.businessdirt.axite.vanadium.renderer.graph

import github.businessdirt.axite.vanadium.core.dag.DirectedAcyclicGraph
import github.businessdirt.axite.vanadium.vulkan.Context
import github.businessdirt.axite.vanadium.vulkan.commands.*
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkClearValue

class RenderGraph(
    context: Context
) : DirectedAcyclicGraph<RenderPassData>() {

    val registry = ResourceRegistry(context)
    val frameContext = RenderFrameContext()

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

    fun execute(commandBuffer: CommandBuffer) {
        frameContext.reset()

        sortedNodes.forEach { node ->
            val passNode = node as RenderPassNode

            // Transition resources
            passNode.writeResources.forEach { res ->
                val attachment = registry[res]
                val targetLayout = if (attachment.isDepth) {
                    VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL
                } else {
                    VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL
                }
                commandBuffer.transitionLayout(attachment, targetLayout)
            }

            passNode.readResources.forEach { res ->
                val attachment = registry[res]
                commandBuffer.transitionLayout(attachment, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
            }

            // Prepare Color Attachments
            val colorAttachments = passNode.writeResources
                .filter { !registry[it].isDepth } // Filter names first
                .map { res ->
                    val attachment = registry[res]
                    val isFirst = frameContext.isFirstWrite(res)

                    val loadOp = when {
                        isFirst && passNode.data.clearColor != null -> VK_ATTACHMENT_LOAD_OP_CLEAR
                        isFirst -> VK_ATTACHMENT_LOAD_OP_DONT_CARE
                        else -> VK_ATTACHMENT_LOAD_OP_LOAD
                    }

                    AttachmentRenderSettings(attachment, loadOp)
                }

            // Prepare Depth Attachment
            val depthResName = passNode.writeResources.find { registry[it].isDepth }
            val depthSettings = depthResName?.let { name ->
                val attachment = registry[name]
                val isFirst = frameContext.isFirstWrite(name)

                val loadOp = when {
                    isFirst && passNode.data.clearDepth != null -> VK_ATTACHMENT_LOAD_OP_CLEAR
                    isFirst -> VK_ATTACHMENT_LOAD_OP_DONT_CARE // Use DONT_CARE if first use and no clear
                    else -> VK_ATTACHMENT_LOAD_OP_LOAD
                }
                AttachmentRenderSettings(attachment, loadOp)
            }

            // Scoped Rendering
            if (colorAttachments.isNotEmpty() || depthSettings != null) {
                // Get dimensions from the first available attachment
                val representative = colorAttachments.firstOrNull()?.attachment ?: depthSettings!!.attachment
                val width = representative.width
                val height = representative.height

                commandBuffer.beginRendering(
                    width, height,
                    colorAttachments,
                    depthSettings, // Fixed: passing the settings object we just built
                    passNode.data.clearColor,
                    passNode.data.clearDepth
                )

                commandBuffer.setViewport(width.toFloat(), height.toFloat())
                commandBuffer.setScissor(width, height)

                // Execute the actual draw calls
                passNode.execute(commandBuffer)

                commandBuffer.endRendering()
            } else {
                // If it's a compute-only pass or something without attachments
                passNode.execute(commandBuffer)
            }
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

    fun clear() = registry.clear()
}