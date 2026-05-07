package github.businessdirt.axite.vanadium.vulkan.commands

import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.Handle
import github.businessdirt.axite.vanadium.vulkan.device.DeviceQueue
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkCommandPoolCreateInfo
import org.lwjgl.vulkan.VkDevice

class CommandPool(
    private val device: VkDevice,
    val queueFamilyIndex: Int,
    supportReset: Boolean = false
) : Handle<Long>() {

    override val handle: Long = memoryStack { stack ->
        val commandPoolCreateInfo = VkCommandPoolCreateInfo.calloc(stack).`sType$Default`()
            .queueFamilyIndex(queueFamilyIndex)
        if (supportReset) commandPoolCreateInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)

        stack.createHandle({ "Failed to create command pool" }) { pCommandPool ->
            vkCreateCommandPool(device, commandPoolCreateInfo, null, pCommandPool)
        }
    }

    /**
     * Allocates a single [CommandBuffer] from this pool.
     */
    fun allocate(primary: Boolean = true, oneTimeSubmit: Boolean = false): CommandBuffer {
        return CommandBuffer(device, this, primary, oneTimeSubmit)
    }

    /**
     * Executes a one-time command on the given [queue].
     * The command buffer is allocated, recorded, submitted, waited on, and then destroyed.
     */
    fun executeTransient(queue: DeviceQueue, block: CommandBuffer.() -> Unit) {
        val commandBuffer = allocate(primary = true, oneTimeSubmit = true)
        commandBuffer.record {
            block()
        }
        commandBuffer.submitAndWait(queue)
        commandBuffer.close()
    }

    override fun destroy() = vkDestroyCommandPool(device, handle, null)
    fun reset() = vkResetCommandPool(device, handle, 0)
}
