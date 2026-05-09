package github.businessdirt.axite.vanadium.renderer.graph

import org.lwjgl.vulkan.VkClearValue

data class RenderPassData(
    val name: String,
    val isGraphicsPass: Boolean = true,
    var clearColor: VkClearValue? = null,
    var clearDepth: VkClearValue? = null
)