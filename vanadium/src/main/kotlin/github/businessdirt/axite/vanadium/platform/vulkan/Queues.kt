package github.businessdirt.axite.vanadium.platform.vulkan

import github.businessdirt.axite.vanadium.utils.createPointer
import github.businessdirt.axite.vanadium.utils.getPointer
import github.businessdirt.axite.vanadium.utils.memoryStack
import org.lwjgl.vulkan.VK13.VK_QUEUE_GRAPHICS_BIT
import org.lwjgl.vulkan.VK13.vkGetDeviceQueue
import org.lwjgl.vulkan.VK13.vkQueueWaitIdle
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkQueue
import kotlin.collections.indexOfFirst

sealed class DeviceQueue(
    val queueFamilyIndex: Int,
    vkDevice: VkDevice,
    queueIndex: Int = 0
) : VulkanHandle<VkQueue>() {

    override val handle: VkQueue = memoryStack { stack ->
        val queueHandle = stack.getPointer { vkGetDeviceQueue(vkDevice, queueFamilyIndex, queueIndex, it)  }
        VkQueue(queueHandle, vkDevice)
    }

    fun waitIdle() = vkQueueWaitIdle(handle)
    override fun destroy() {}
}

class GraphicsQueue(
    physicalDevice: PhysicalDevice,
    vkDevice: VkDevice,
    queueIndex: Int = 0
) : DeviceQueue(getGraphicsQueueFamilyIndex(physicalDevice), vkDevice, queueIndex) {

    companion object {
        private fun getGraphicsQueueFamilyIndex(physicalDevice: PhysicalDevice): Int {
            val queueProps = physicalDevice.queueFamilyProperties
            val index = (0 until queueProps.capacity()).indexOfFirst { i ->
                (queueProps[i].queueFlags() and VK_QUEUE_GRAPHICS_BIT) != 0
            }

            check(index >= 0) { "Failed to get graphics Queue family index" }
            return index
        }
    }
}