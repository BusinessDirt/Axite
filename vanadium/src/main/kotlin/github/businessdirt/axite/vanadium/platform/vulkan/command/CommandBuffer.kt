package github.businessdirt.axite.vanadium.platform.vulkan.command

import github.businessdirt.axite.vanadium.platform.vulkan.Context
import github.businessdirt.axite.vanadium.platform.vulkan.Device
import github.businessdirt.axite.vanadium.platform.vulkan.DeviceQueue
import github.businessdirt.axite.vanadium.platform.vulkan.VulkanHandle
import github.businessdirt.axite.vanadium.platform.vulkan.synchronization.Fence
import github.businessdirt.axite.vanadium.utils.createPointer
import github.businessdirt.axite.vanadium.utils.memoryStack
import github.businessdirt.axite.vanadium.utils.vkCheck
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*


class CommandBuffer(
    val commandPool: CommandPool,
    val primary: Boolean,
    val oneTimeSubmit: Boolean,
) : VulkanHandle<VkCommandBuffer>() {

    override val handle: VkCommandBuffer = memoryStack { stack ->
        val commandBufferAllocateInfo = VkCommandBufferAllocateInfo.calloc(stack)
                .`sType$Default`()
                .commandPool(commandPool.handle)
                .level(if (primary) VK_COMMAND_BUFFER_LEVEL_PRIMARY else VK_COMMAND_BUFFER_LEVEL_SECONDARY)
                .commandBufferCount(1)

        val commandBufferHandle = stack.createPointer({ "Failed to allocate command buffer" }) {
            vkAllocateCommandBuffers(Context.device.handle, commandBufferAllocateInfo, it)
        }

        VkCommandBuffer(commandBufferHandle, Context.device.handle)

    }

    fun beginRecording(inheritanceInfo: InheritanceInfo? = null) = memoryStack { stack ->
        val commandBufferBeginInfo = VkCommandBufferBeginInfo.calloc(stack).`sType$Default`()
        if (oneTimeSubmit) commandBufferBeginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)

        if (!primary) {
            val info = inheritanceInfo ?: throw RuntimeException("Secondary buffers must declare inheritance info")

            val renderingInfo = VkCommandBufferInheritanceRenderingInfo.calloc(stack)
                .`sType$Default`()
                .depthAttachmentFormat(info.depthFormat)
                .pColorAttachmentFormats(stack.ints(*info.colorFormats))
                .rasterizationSamples(info.rasterizationSamples)

            val inheritanceInfo = VkCommandBufferInheritanceInfo.calloc(stack)
                .`sType$Default`()
                .pNext(renderingInfo.address()) // Link rendering info via pNext

            commandBufferBeginInfo.pInheritanceInfo(inheritanceInfo)
        }

        vkCheck(vkBeginCommandBuffer(handle, commandBufferBeginInfo)) {
            "Failed to begin command buffer"
        }
    }

    fun endRecording() = vkCheck(vkEndCommandBuffer(handle)) { "Failed to end command buffer" }
    fun reset() = vkResetCommandBuffer(handle, VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT)

    /**
     * Executes a recording block. Automatically calls beginRecording and endRecording.
     */
    inline fun record(
        inheritanceInfo: InheritanceInfo? = null,
        block: CommandBuffer.() -> Unit
    ) {
        beginRecording(inheritanceInfo)
        try {
            this.block()
        } finally {
            endRecording()
        }
    }

    fun submitAndWait(device: Device, queue: DeviceQueue) {
        val fence = Fence(false)

        memoryStack { stack ->
            val commandBufferSubmitInfos = VkCommandBufferSubmitInfo.calloc(1, stack)
                .`sType$Default`()
                .commandBuffer(handle)

            queue.submit(commandBuffers = commandBufferSubmitInfos, fence = fence)
        }

        fence.wait()
        fence.cleanup()
    }

    override fun destroy() = vkFreeCommandBuffers(Context.device.handle, commandPool.handle, handle)

    data class InheritanceInfo(
        val depthFormat: Int,
        val colorFormats: IntArray,
        val rasterizationSamples: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as InheritanceInfo

            if (depthFormat != other.depthFormat) return false
            if (rasterizationSamples != other.rasterizationSamples) return false
            if (!colorFormats.contentEquals(other.colorFormats)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = depthFormat
            result = 31 * result + rasterizationSamples
            result = 31 * result + colorFormats.contentHashCode()
            return result
        }
    }
}