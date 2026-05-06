package github.businessdirt.axite.vanadium.vulkan.device

import github.businessdirt.axite.vanadium.core.utils.getPointer
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.core.utils.vkCheck
import github.businessdirt.axite.vanadium.vulkan.Handle
import github.businessdirt.axite.vanadium.vulkan.synchronization.Fence
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*

sealed class DeviceQueue(
    device: VkDevice,
    queueIndex: Int = 0
) : Handle<VkQueue>() {

    abstract val queueFamilyIndex: Int

    override val handle: VkQueue = memoryStack { stack ->
        val queueHandle = stack.getPointer { vkGetDeviceQueue(device, queueFamilyIndex, queueIndex, it)  }
        VkQueue(queueHandle, device)
    }

    fun submit(
        commandBuffers: VkCommandBufferSubmitInfo.Buffer,
        waitSemaphores: VkSemaphoreSubmitInfo.Buffer? = null,
        signalSemaphores: VkSemaphoreSubmitInfo.Buffer? = null,
        fence: Fence? = null
    ) = memoryStack { stack ->
        val submitInfo = VkSubmitInfo2.calloc(1, stack).`sType$Default`()
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
