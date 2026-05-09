package github.businessdirt.axite.vanadium.renderer.graph

import github.businessdirt.axite.vanadium.core.dag.DirectedAcyclicGraph
import github.businessdirt.axite.vanadium.vulkan.Context
import github.businessdirt.axite.vanadium.vulkan.commands.*
import github.businessdirt.axite.vanadium.vulkan.resources.Image
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkClearValue

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

                commandBuffer.setViewport(width.toFloat(), height.toFloat())
                commandBuffer.setScissor(width, height)
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