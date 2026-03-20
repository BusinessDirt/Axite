package github.businessdirt.axite.vanadium.graph.scene

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo
import org.lwjgl.vulkan.VkVertexInputAttributeDescription
import org.lwjgl.vulkan.VkVertexInputBindingDescription

object VertexDefinition {
    const val POSITION_COMPONENTS = 3
    const val TEXTURE_COORDINATE_COMPONENTS = 2
    private const val NUMBER_OF_ATTRIBUTES = 2

    // Total size of a single vertex in bytes (3 floats for pos + 2 floats for uv = 20 bytes)
    const val SIZEOF = (POSITION_COMPONENTS + TEXTURE_COORDINATE_COMPONENTS) * Float.SIZE_BYTES

    fun createInputState(stack: MemoryStack): VkPipelineVertexInputStateCreateInfo {
        val attributes = VkVertexInputAttributeDescription.calloc(NUMBER_OF_ATTRIBUTES, stack)

        // 0: Position (vec3)
        attributes[0].binding(0).location(0)
            .format(VK_FORMAT_R32G32B32_SFLOAT)
            .offset(0)

        // 1: Texture Coordinates (vec2)
        attributes[1].binding(0).location(1)
            .format(VK_FORMAT_R32G32_SFLOAT)
            .offset(POSITION_COMPONENTS * Float.SIZE_BYTES) // Starts right after the 3 position floats

        // Allocate space for 1 binding description on the stack
        val bindings = VkVertexInputBindingDescription.calloc(1, stack)
        bindings[0].binding(0)
            .stride(SIZEOF) // Jump 20 bytes to get to the next vertex
            .inputRate(VK_VERTEX_INPUT_RATE_VERTEX)

        // Combine into the final state struct
        return VkPipelineVertexInputStateCreateInfo.calloc(stack)
            .`sType$Default`()
            .pVertexBindingDescriptions(bindings)
            .pVertexAttributeDescriptions(attributes)
    }
}