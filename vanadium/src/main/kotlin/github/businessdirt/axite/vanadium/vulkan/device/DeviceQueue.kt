package github.businessdirt.axite.vanadium.vulkan.device

import github.businessdirt.axite.vanadium.core.utils.getPointer
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.core.utils.vkCheck
import github.businessdirt.axite.vanadium.vulkan.Handle
import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer
import github.businessdirt.axite.vanadium.vulkan.commands.CommandPool
import github.businessdirt.axite.vanadium.vulkan.synchronization.Fence
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*

sealed class DeviceQueue(
    protected val device: VkDevice,
    queueIndex: Int = 0
) : Handle<VkQueue>() {

    abstract val queueFamilyIndex: Int

    override val handle: VkQueue = memoryStack { stack ->
        val queueHandle = stack.getPointer { vkGetDeviceQueue(device, queueFamilyIndex, queueIndex, it)  }
        VkQueue(queueHandle, device)
    }

    /**
     * Internal command pool for transient operations on this queue.
     * Initialized lazily to ensure queueFamilyIndex is available.
     */
    private val commandPoolDelegate = lazy { CommandPool(device, queueFamilyIndex, supportReset = true) }
    val commandPool: CommandPool by commandPoolDelegate

    fun submit(
        commandBuffers: VkCommandBufferSubmitInfo.Buffer,
        waitSemaphores: VkSemaphoreSubmitInfo.Buffer? = null,
        signalSemaphores: VkSemaphoreSubmitInfo.Buffer? = null,
        fence: Fence? = null
    ) = memoryStack { stack ->
        val submitInfo = VkSubmitInfo2.calloc(1, stack).`sType$Default`()
            .pCommandBufferInfos(commandBuffers)
            .pSignalSemaphoreInfos(signalSemaphores)
            .pWaitSemaphoreInfos(waitSemaphores)

        val fenceHandle = fence?.handle ?: VK_NULL_HANDLE
        vkCheck(vkQueueSubmit2(handle, submitInfo, fenceHandle)) {
            "Failed to submit command to queue"
        }
    }

    /**
     * Executes a one-time command buffer on this queue.
     */
    fun execute(block: CommandBuffer.() -> Unit) = commandPool.executeTransient(this, block)

    fun waitIdle() = vkQueueWaitIdle(handle)

    override fun destroy() {
        // CommandPool is a Handle and needs to be closed if it was used
        if (commandPoolDelegate.isInitialized()) commandPool.close()
    }
}
