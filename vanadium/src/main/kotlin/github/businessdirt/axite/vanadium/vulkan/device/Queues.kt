package github.businessdirt.axite.vanadium.vulkan.device

import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.surface.Surface
import org.lwjgl.vulkan.KHRSurface
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkDevice

class GraphicsQueue(
    device: VkDevice,
    physicalDevice: PhysicalDevice,
    queueIndex: Int = 0
) : DeviceQueue(device, queueIndex) {

    override val queueFamilyIndex: Int = physicalDevice.queueFamilyProperties.use { queueProps ->
        val index = (0 until queueProps.capacity()).indexOfFirst { i ->
            (queueProps[i].queueFlags() and VK_QUEUE_GRAPHICS_BIT) != 0
        }

        check(index >= 0) { "Failed to get graphics Queue family index" }
        index
    }
}

class ComputeQueue(
    device: VkDevice,
    physicalDevice: PhysicalDevice,
    queueIndex: Int = 0
) : DeviceQueue(device, queueIndex) {

    override val queueFamilyIndex: Int = physicalDevice.queueFamilyProperties.use { queueProps ->
        val index = (0 until queueProps.capacity()).indexOfFirst { i ->
            (queueProps[i].queueFlags() and VK_QUEUE_COMPUTE_BIT) != 0
        }

        check(index >= 0) { "Failed to get compute Queue family index" }
        index
    }
}

class PresentQueue(
    device: VkDevice,
    physicalDevice: PhysicalDevice,
    surface: Surface,
    queueIndex: Int = 0
) : DeviceQueue(device, queueIndex) {

    override val queueFamilyIndex: Int = memoryStack { stack ->
        val pSupported = stack.mallocInt(1)

        (0 until physicalDevice.queueFamilyProperties.capacity()).firstOrNull { i ->
            KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(
                physicalDevice.handle,
                i,
                surface.handle,
                pSupported
            )
            pSupported[0] == VK_TRUE
        } ?: throw RuntimeException("Failed to get Presentation Queue family index")
    }
}
