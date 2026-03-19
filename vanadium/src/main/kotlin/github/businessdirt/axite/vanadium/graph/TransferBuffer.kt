package github.businessdirt.axite.vanadium.graph

import github.businessdirt.axite.vanadium.platform.vulkan.command.CommandBuffer
import github.businessdirt.axite.vanadium.platform.vulkan.resources.Buffer
import github.businessdirt.axite.vanadium.utils.memoryStack
import org.lwjgl.vulkan.VK10.vkCmdCopyBuffer
import org.lwjgl.vulkan.VkBufferCopy

data class TransferBuffer(
    val srcBuffer: Buffer,
    val dstBuffer: Buffer,
) {
    fun record(commandBuffer: CommandBuffer) = memoryStack { stack ->
        val copyRegion = VkBufferCopy.calloc(1, stack)
            .srcOffset(0).dstOffset(0).size(srcBuffer.requestedSize)
        vkCmdCopyBuffer(commandBuffer.handle, srcBuffer.handle, dstBuffer.handle, copyRegion)
    }
}