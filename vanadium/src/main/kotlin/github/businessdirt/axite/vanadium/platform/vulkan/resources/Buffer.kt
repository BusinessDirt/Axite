package github.businessdirt.axite.vanadium.platform.vulkan.resources

import github.businessdirt.axite.vanadium.platform.vulkan.Context
import github.businessdirt.axite.vanadium.platform.vulkan.VulkanHandle
import github.businessdirt.axite.vanadium.utils.VulkanUtils
import github.businessdirt.axite.vanadium.utils.createHandle
import github.businessdirt.axite.vanadium.utils.createPointer
import github.businessdirt.axite.vanadium.utils.memoryStack
import github.businessdirt.axite.vanadium.utils.runIfNonNull
import github.businessdirt.axite.vanadium.utils.vkCheck
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE
import org.lwjgl.vulkan.VK10.vkAllocateMemory
import org.lwjgl.vulkan.VK10.vkBindBufferMemory
import org.lwjgl.vulkan.VK10.vkCreateBuffer
import org.lwjgl.vulkan.VK10.vkDestroyBuffer
import org.lwjgl.vulkan.VK10.vkFreeMemory
import org.lwjgl.vulkan.VK10.vkGetBufferMemoryRequirements
import org.lwjgl.vulkan.VK10.vkMapMemory
import org.lwjgl.vulkan.VK10.vkUnmapMemory
import org.lwjgl.vulkan.VkBufferCreateInfo
import org.lwjgl.vulkan.VkMemoryAllocateInfo
import org.lwjgl.vulkan.VkMemoryRequirements
import kotlin.properties.Delegates

class Buffer(
    val requestedSize: Long,
    usage: Int,
    reqMask: Int
) : VulkanHandle<Long>() {
    override val handle: Long = memoryStack { stack ->
        val bufferCreateInfo = VkBufferCreateInfo.calloc(stack)
            .`sType$Default`()
            .size(requestedSize)
            .usage(usage)
            .sharingMode(VK_SHARING_MODE_EXCLUSIVE)

        stack.createHandle({ "Failed to create buffer" }) {
            vkCreateBuffer(Context.device.handle, bufferCreateInfo, null, it)
        }
    }

    var allocationSize by Delegates.notNull<Long>()
        private set

    val memory: Long = memoryStack { stack ->
        // Get Memory Requirements
        val memReqs = VkMemoryRequirements.calloc(stack)
        vkGetBufferMemoryRequirements(Context.device.handle, handle, memReqs)

        // Allocate Memory
        val memAlloc = VkMemoryAllocateInfo.calloc(stack)
            .`sType$Default`()
            .allocationSize(memReqs.size())
            .memoryTypeIndex(
                VulkanUtils.memoryTypeFromProperties(memReqs.memoryTypeBits(), reqMask)
            )

        allocationSize = memAlloc.allocationSize()
        stack.createHandle({ "Failed to allocate memory" }) {
            vkAllocateMemory(Context.device.handle, memAlloc, null, it)
        }
    }

    var mappedMemory: Long = NULL
        private set

    init {
        vkCheck(vkBindBufferMemory(Context.device.handle, handle, memory, 0)) {
            "Failed to bind buffer memory"
        }
    }

    fun map(): Long = when {
        mappedMemory != NULL -> mappedMemory
        else -> {
            memoryStack { stack ->
                mappedMemory = stack.createPointer({ "Failed to map Buffer" }) {
                    vkMapMemory(Context.device.handle, memory, 0, allocationSize, 0, it)
                }
            }
            mappedMemory
        }
    }

    fun unmap() = mappedMemory.runIfNonNull {
        vkUnmapMemory(Context.device.handle, memory)
        mappedMemory = NULL
    }

    override fun destroy() {
        val device = Context.device.handle
        vkDestroyBuffer(device, handle, null)
        vkFreeMemory(device, memory, null)
    }
}