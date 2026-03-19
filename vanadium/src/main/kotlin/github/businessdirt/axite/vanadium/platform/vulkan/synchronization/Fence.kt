package github.businessdirt.axite.vanadium.platform.vulkan.synchronization

import github.businessdirt.axite.vanadium.platform.vulkan.Context
import github.businessdirt.axite.vanadium.platform.vulkan.VulkanHandle
import github.businessdirt.axite.vanadium.utils.createHandle
import github.businessdirt.axite.vanadium.utils.memoryStack
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkFenceCreateInfo

class Fence(signaled: Boolean) : VulkanHandle<Long>() {
    override val handle: Long = memoryStack { stack ->
        val fenceCreateInfo = VkFenceCreateInfo.calloc(stack)
            .`sType$Default`()
            .flags(if (signaled) VK_FENCE_CREATE_SIGNALED_BIT else 0)

        stack.createHandle({ "Failed to create fence" }) {
            vkCreateFence(Context.device.handle, fenceCreateInfo, null, it)
        }
    }

    fun reset() = vkResetFences(Context.device.handle, handle)
    fun wait(timeout: Long = Long.MAX_VALUE) = vkWaitForFences(Context.device.handle, handle, true, timeout)

    override fun destroy() = vkDestroyFence(Context.device.handle, handle, null)
}