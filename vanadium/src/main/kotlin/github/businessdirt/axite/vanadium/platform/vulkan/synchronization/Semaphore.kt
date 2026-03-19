package github.businessdirt.axite.vanadium.platform.vulkan.synchronization

import github.businessdirt.axite.vanadium.platform.vulkan.Context
import github.businessdirt.axite.vanadium.platform.vulkan.VulkanHandle
import github.businessdirt.axite.vanadium.utils.createHandle
import github.businessdirt.axite.vanadium.utils.memoryStack
import org.lwjgl.vulkan.VK13.vkCreateSemaphore
import org.lwjgl.vulkan.VK13.vkDestroySemaphore
import org.lwjgl.vulkan.VkSemaphoreCreateInfo

class Semaphore : VulkanHandle<Long>() {

    override val handle: Long = memoryStack { stack ->
        val semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack).`sType$Default`()
        stack.createHandle({ "Failed to create Semaphore" }) {
            vkCreateSemaphore(Context.device.handle, semaphoreCreateInfo, null, it)
        }
    }

    override fun destroy() = vkDestroySemaphore(Context.device.handle, handle, null)
}