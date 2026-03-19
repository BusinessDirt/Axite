package github.businessdirt.axite.vanadium.platform.vulkan.command

import github.businessdirt.axite.vanadium.platform.vulkan.Context
import github.businessdirt.axite.vanadium.platform.vulkan.VulkanHandle
import github.businessdirt.axite.vanadium.utils.createHandle
import github.businessdirt.axite.vanadium.utils.memoryStack
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkCommandPoolCreateInfo

class CommandPool(
    queueFamilyIndex: Int,
    supportReset: Boolean,
) : VulkanHandle<Long>() {

    override val handle: Long = memoryStack { stack ->
        val commandPoolInfo = VkCommandPoolCreateInfo.calloc()
            .`sType$Default`()
            .queueFamilyIndex(queueFamilyIndex)
        if (supportReset) commandPoolInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)

        stack.createHandle({ "Failed to create command pool" }) { longBuffer ->
            vkCreateCommandPool(Context.device.handle, commandPoolInfo, null, longBuffer)
        }
    }

    override fun destroy() = vkDestroyCommandPool(Context.device.handle, handle, null)
    fun reset() = vkResetCommandPool(Context.device.handle, handle, 0)
}