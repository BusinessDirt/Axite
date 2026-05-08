package github.businessdirt.axite.vanadium.renderer.graph

import github.businessdirt.axite.vanadium.core.dag.Node
import github.businessdirt.axite.vanadium.core.dag.ResourceUser

class RenderPassNode(
    val name: String,
    override val readResources: Set<String>,
    override val writeResources: Set<String>,
    private val action: () -> Unit
) : Node<RenderPassData>(RenderPassData(name)), ResourceUser {

    /**
     * Called by the DirectedAcyclicGraph during the execution phase.
     */
    override fun execute() {
        // TODO: begin vulkan render pass
        // TODO: Label for GPU debugging (e.g., vkCmdBeginDebugUtilsLabelEXT)
        action()
    }

}