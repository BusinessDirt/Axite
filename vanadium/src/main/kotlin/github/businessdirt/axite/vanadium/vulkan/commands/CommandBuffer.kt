package github.businessdirt.axite.vanadium.vulkan.commands

import github.businessdirt.axite.vanadium.core.utils.createPointer
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.core.utils.vkCheck
import github.businessdirt.axite.vanadium.vulkan.Handle
import github.businessdirt.axite.vanadium.vulkan.device.DeviceQueue
import github.businessdirt.axite.vanadium.vulkan.synchronization.Fence
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*

class CommandBuffer(
    private val device: VkDevice,
    val commandPool: CommandPool,
    val primary: Boolean,
    val oneTimeSubmit: Boolean,
) : Handle<VkCommandBuffer>() {

    override val handle: VkCommandBuffer = memoryStack { stack ->
        val commandBufferAllocateInfo = VkCommandBufferAllocateInfo.calloc(stack)
            .`sType$Default`()
            .commandPool(commandPool.handle)
            .level(if (primary) VK_COMMAND_BUFFER_LEVEL_PRIMARY else VK_COMMAND_BUFFER_LEVEL_SECONDARY)
            .commandBufferCount(1)

        val commandBufferHandle = stack.createPointer({ "Failed to allocate command buffer" }) {
            vkAllocateCommandBuffers(device, commandBufferAllocateInfo, it)
        }

        VkCommandBuffer(commandBufferHandle, device)
    }

    /**
     * Begins recording commands into the buffer.
     * [inheritanceInfo] must be provided if this is a secondary command buffer.
     */
    fun begin(inheritanceInfo: InheritanceInfo? = null) = memoryStack { stack ->
        val commandBufferBeginInfo = VkCommandBufferBeginInfo.calloc(stack).`sType$Default`()
        if (oneTimeSubmit) commandBufferBeginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)

        if (!primary) {
            val info = inheritanceInfo ?: throw RuntimeException("Secondary buffers must declare inheritance info")

            val renderingInfo = VkCommandBufferInheritanceRenderingInfo.calloc(stack)
                .`sType$Default`()
                .depthAttachmentFormat(info.depthFormat)
                .pColorAttachmentFormats(stack.ints(*info.colorFormats))
                .rasterizationSamples(info.rasterizationSamples)

            val vkInheritanceInfo = VkCommandBufferInheritanceInfo.calloc(stack)
                .`sType$Default`()
                .pNext(renderingInfo.address())

            commandBufferBeginInfo.pInheritanceInfo(vkInheritanceInfo)
        }

        vkCheck(vkBeginCommandBuffer(handle, commandBufferBeginInfo)) {
            "Failed to begin command buffer"
        }
    }

    /**
     * Ends recording commands.
     */
    fun end() = vkCheck(vkEndCommandBuffer(handle)) { "Failed to end command buffer" }

    /**
     * Resets the command buffer, allowing it to be recorded again.
     */
    fun reset() = vkResetCommandBuffer(handle, VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT)

    /**
     * Executes a recording block. Automatically calls [begin] and [end].
     */
    inline fun record(
        inheritanceInfo: InheritanceInfo? = null,
        block: CommandBuffer.() -> Unit
    ): CommandBuffer {
        begin(inheritanceInfo)
        try {
            this.block()
        } finally {
            end()
        }
        return this
    }

    /**
     * Submits the command buffer to the given [queue].
     * Optional [fence] can be provided to track completion.
     */
    fun submit(queue: DeviceQueue, fence: Fence? = null) = memoryStack { stack ->
        val commandBufferSubmitInfos = VkCommandBufferSubmitInfo.calloc(1, stack)
            .`sType$Default`()
            .commandBuffer(handle)

        queue.submit(commandBuffers = commandBufferSubmitInfos, fence = fence)
    }

    /**
     * Submits the command buffer to the given [queue] and waits for it to complete using a temporary fence.
     */
    fun submitAndWait(queue: DeviceQueue) {
        val fence = Fence(device, false)
        submit(queue, fence)
        fence.wait()
        fence.close()
    }

    override fun destroy() = vkFreeCommandBuffers(device, commandPool.handle, handle)

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
