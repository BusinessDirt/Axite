package github.businessdirt.axite.vanadium.renderer.graph

import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer

class RenderGraphBuilder(private val graph: RenderGraph) {

    fun pass(name: String, block: PassBuilder.() -> Unit) {
        val builder = PassBuilder(name)
        builder.block()

        val node = RenderPassNode(
            name = builder.name,
            readResources = builder.reads,
            writeResources = builder.writes,
            action = builder.action ?: { _ -> }
        ).apply {
            data.clearColorValue = builder.clearColor
            data.clearDepthValue = builder.clearDepth
        }

        graph.nodes.add(node)
    }
}

class PassBuilder(val name: String) {
    val reads = mutableSetOf<String>()
    val writes = mutableSetOf<String>()

    var clearColor: ClearColorValue? = null
    var clearDepth: Float? = null

    internal var action: ((CommandBuffer) -> Unit)? = null

    fun read(vararg resourceNames: String) = reads.addAll(resourceNames)
    fun writes(vararg resourceNames: String) = writes.addAll(resourceNames)

    fun clearColor(red: Float, green: Float, blue: Float, alpha: Float) {
        clearColor = ClearColorValue(red, green, blue, alpha)
    }

    fun clearDepth(depth: Float) {
        clearDepth = depth
    }

    fun pipeline(block: (CommandBuffer) -> Unit) {
        this.action = block
    }
}