package github.businessdirt.axite.vanadium.vulkan.resources

import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.Handle
import org.lwjgl.vulkan.VK13.vkCreateShaderModule
import org.lwjgl.vulkan.VK13.vkDestroyShaderModule
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkShaderModuleCreateInfo
import java.nio.ByteBuffer

class ShaderModule(
    private val device: VkDevice,
    val stage: Int,
    pCode: ByteBuffer
) : Handle<Long>() {

    override val handle: Long

    init {
        // Validation remains here as a safety guard for the Vulkan API
        if (pCode.remaining() % 4 != 0)
            throw IllegalArgumentException("SPIR-V shader buffer size is not a multiple of 4")

        handle = memoryStack { stack ->
            val createInfo = VkShaderModuleCreateInfo.calloc(stack).`sType$Default`()
                .pCode(pCode)

            stack.createHandle({ "Failed to create shader module from buffer" }) {
                vkCreateShaderModule(device, createInfo, null, it)
            }
        }
    }

    override fun destroy() = vkDestroyShaderModule(device, handle, null)
}