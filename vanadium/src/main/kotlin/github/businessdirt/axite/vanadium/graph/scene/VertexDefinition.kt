package github.businessdirt.axite.vanadium.graph.scene

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo
import org.lwjgl.vulkan.VkVertexInputAttributeDescription
import org.lwjgl.vulkan.VkVertexInputBindingDescription

object VertexDefinition {
    private const val POSITION_COMPONENTS = 3
    const val SIZEOF = POSITION_COMPONENTS * Float.SIZE_BYTES

    fun createInputState(stack: MemoryStack): VkPipelineVertexInputStateCreateInfo {
        val attributes = VkVertexInputAttributeDescription.calloc(1, stack)
        attributes[0].binding(0).location(0)
            .format(VK_FORMAT_R32G32B32_SFLOAT)
            .offset(0)

        val bindings = VkVertexInputBindingDescription.calloc(1, stack)
        bindings[0].binding(0).stride(SIZEOF)
            .inputRate(VK_VERTEX_INPUT_RATE_VERTEX)

        return VkPipelineVertexInputStateCreateInfo.calloc(stack).`sType$Default`()
            .pVertexBindingDescriptions(bindings)
            .pVertexAttributeDescriptions(attributes)
    }
}