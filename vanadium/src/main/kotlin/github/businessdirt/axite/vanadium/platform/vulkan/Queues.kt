package github.businessdirt.axite.vanadium.platform.vulkan

import github.businessdirt.axite.vanadium.platform.vulkan.synchronization.Fence
import github.businessdirt.axite.vanadium.utils.getPointer
import github.businessdirt.axite.vanadium.utils.memoryStack
import github.businessdirt.axite.vanadium.utils.vkCheck
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*


sealed class DeviceQueue(
    queueIndex: Int = 0
) : VulkanHandle<VkQueue>() {

    abstract val queueFamilyIndex: Int

    override val handle: VkQueue = memoryStack { stack ->
        val queueHandle = stack.getPointer { vkGetDeviceQueue(Context.device.handle, queueFamilyIndex, queueIndex, it)  }
        VkQueue(queueHandle, Context.device.handle)
    }

    fun submit(
        commandBuffers: VkCommandBufferSubmitInfo.Buffer,
        waitSemaphores: VkSemaphoreSubmitInfo.Buffer? = null,
        signalSemaphores: VkSemaphoreSubmitInfo.Buffer? = null,
        fence: Fence? = null
    ) = memoryStack { stack ->
        val submitInfo = VkSubmitInfo2.calloc(1, stack)
            .`sType$Default`()
            .pCommandBufferInfos(commandBuffers)
            .pSignalSemaphoreInfos(signalSemaphores) // LWJGL handles null Buffers gracefully here
            .pWaitSemaphoreInfos(waitSemaphores)

        val fenceHandle = fence?.handle ?: VK_NULL_HANDLE
        vkCheck(vkQueueSubmit2(handle, submitInfo, fenceHandle)) {
            "Failed to submit command to queue"
        }
    }

    fun waitIdle() = vkQueueWaitIdle(handle)
    override fun destroy() {}
}

class GraphicsQueue(
    queueIndex: Int = 0
) : DeviceQueue( queueIndex) {

    override val queueFamilyIndex: Int = Context.physicalDevice.queueFamilyProperties.use { queueProps ->
        val index = (0 until queueProps.capacity()).indexOfFirst { i ->
            (queueProps[i].queueFlags() and VK_QUEUE_GRAPHICS_BIT) != 0
        }

        check(index >= 0) { "Failed to get graphics Queue family index" }
        index
    }
}

class PresentQueue(
    queueIndex: Int = 0
) : DeviceQueue(queueIndex) {
    override val queueFamilyIndex: Int = memoryStack { stack ->
        val physicalDevice = Context.physicalDevice
        val pSupported = stack.mallocInt(1)

        (0..< physicalDevice.queueFamilyProperties.capacity()).firstOrNull { i ->
            KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(
                physicalDevice.handle,
                i,
                Context.surface.handle,
                pSupported
            )
            pSupported[0] == VK_TRUE
        } ?: throw RuntimeException("Failed to get Presentation Queue family index")
    }
}