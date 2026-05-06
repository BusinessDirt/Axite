package github.businessdirt.axite.vanadium.vulkan.synchronization

import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.Handle
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkFenceCreateInfo

class Fence(
    private val device: VkDevice,
    signaled: Boolean
) : Handle<Long>() {
    override val handle: Long = memoryStack { stack ->
        val fenceCreateInfo = VkFenceCreateInfo.calloc(stack).`sType$Default`()
            .flags(if (signaled) VK_FENCE_CREATE_SIGNALED_BIT else 0)

        stack.createHandle({ "Failed to create fence" }) {
            vkCreateFence(device, fenceCreateInfo, null, it)
        }
    }

    fun reset() = vkResetFences(device, handle)
    fun wait(timeout: Long = Long.MAX_VALUE) = vkWaitForFences(device, handle, true, timeout)

    override fun destroy() = vkDestroyFence(device, handle, null)
}