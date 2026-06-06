package github.businessdirt.axite.vanadium.renderer.graph

import github.businessdirt.axite.vanadium.core.dag.DirectedAcyclicGraph
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.Context
import github.businessdirt.axite.vanadium.vulkan.commands.*
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkClearValue

class RenderGraph(
    context: Context
) : DirectedAcyclicGraph<RenderPassData>() {

    val registry = ResourceRegistry(context)
    val frameContext = RenderFrameContext()

    fun build(action: RenderGraphBuilder.() -> Unit) {
        nodes.clear()
        val builder = RenderGraphBuilder(this)
        builder.action()
    }

    override fun compile() {
        resolveDependencies()
        super.compile()
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

            // Only transition to SHADER_READ_ONLY_OPTIMAL if the resource is NOT being written to in this pass.
            // If it is being written to, it must stay in COLOR_ATTACHMENT_OPTIMAL (or DEPTH_STENCIL_ATTACHMENT_OPTIMAL).
            passNode.readResources.filter { it !in passNode.writeResources }.forEach { res ->
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
                        isFirst && passNode.data.clearColorValue != null -> VK_ATTACHMENT_LOAD_OP_CLEAR
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
                    isFirst && passNode.data.clearDepthValue != null -> VK_ATTACHMENT_LOAD_OP_CLEAR
                    isFirst -> VK_ATTACHMENT_LOAD_OP_DONT_CARE // Use DONT_CARE if first use and no clear
                    else -> VK_ATTACHMENT_LOAD_OP_LOAD
                }
                AttachmentRenderSettings(attachment, loadOp)
            }

            // Scoped Rendering
            memoryStack { stack ->
                val vkClearColor = passNode.data.clearColorValue?.createVkClearValue(stack)
                val vkClearDepth = passNode.data.clearDepthValue?.let { depthValue ->
                    VkClearValue.calloc(stack).depthStencil { it.depth(depthValue) }
                }

                if (colorAttachments.isNotEmpty() || depthSettings != null) {
                    // Get dimensions from the first available attachment
                    val representative = colorAttachments.firstOrNull()?.attachment ?: depthSettings!!.attachment
                    val width = representative.width
                    val height = representative.height

                    commandBuffer.beginRendering(
                        width, height,
                        colorAttachments,
                        depthSettings,
                        vkClearColor,
                        vkClearDepth
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
    }

    fun use(commandBuffer: CommandBuffer, action: (RenderGraph) -> Unit) {
        nodes.clear()
        resourceLifetimes.clear()

        action(this)

        compile()
        execute(commandBuffer)
    }

    fun clear() = registry.clear()
}