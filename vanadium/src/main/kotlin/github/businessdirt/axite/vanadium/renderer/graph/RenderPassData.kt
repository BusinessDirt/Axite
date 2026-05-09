package github.businessdirt.axite.vanadium.renderer.graph

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VkClearValue

data class RenderPassData(
    val name: String,
    val isGraphicsPass: Boolean = true,
    var clearColorValue: ClearColorValue? = null,
    var clearDepthValue: Float? = null,
)

data class ClearColorValue(
    val red: Float,
    val green: Float,
    val blue: Float,
    val alpha: Float
) {
    fun createVkClearValue(stack: MemoryStack): VkClearValue =
        VkClearValue.calloc(stack).color { color ->
            color.float32(0, red)
            color.float32(1, green)
            color.float32(2, blue)
            color.float32(3, alpha)
        }
}