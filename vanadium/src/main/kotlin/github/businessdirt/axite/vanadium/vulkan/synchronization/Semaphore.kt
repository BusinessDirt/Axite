package github.businessdirt.axite.vanadium.vulkan.synchronization

import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.Handle
import org.lwjgl.vulkan.VK13.vkCreateSemaphore
import org.lwjgl.vulkan.VK13.vkDestroySemaphore
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkSemaphoreCreateInfo

class Semaphore(
    private val device: VkDevice,
) : Handle<Long>() {

    override val handle: Long = memoryStack { stack ->
        val semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack).`sType$Default`()
        stack.createHandle({ "Failed to create Semaphore" }) {
            vkCreateSemaphore(device, semaphoreCreateInfo, null, it)
        }
    }

    override fun destroy() = vkDestroySemaphore(device, handle, null)
}