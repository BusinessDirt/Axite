package github.businessdirt.axite.vanadium.renderer.graph

import github.businessdirt.axite.vanadium.core.dag.Node
import github.businessdirt.axite.vanadium.core.dag.ResourceUser
import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer

class RenderPassNode(
    val name: String,
    override val readResources: Set<String>,
    override val writeResources: Set<String>,
    private val action: (CommandBuffer) -> Unit
) : Node<RenderPassData>(RenderPassData(name)), ResourceUser {

    /**
     * Called by the DirectedAcyclicGraph during the execution phase.
     */
    fun execute(commandBuffer: CommandBuffer) {
        // TODO: Label for GPU debugging (e.g., vkCmdBeginDebugUtilsLabelEXT)
        action(commandBuffer)
    }

    override fun execute() {
        throw UnsupportedOperationException("Use execute(CommandBuffer) instead")
    }
}