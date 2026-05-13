package github.businessdirt.axite.vanadium.vulkan.resources

import github.businessdirt.axite.vanadium.core.utils.VulkanUtils.memoryTypeFromProperties
import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.createPointer
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.core.utils.vkCheck
import github.businessdirt.axite.vanadium.vulkan.Handle
import github.businessdirt.axite.vanadium.vulkan.device.PhysicalDevice
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*

class Buffer(
    private val device: VkDevice,
    private val physicalDevice: PhysicalDevice,
    val size: Long,
    usage: Int,
    properties: Int
) : Handle<Long>() {

    override val handle: Long

    val memory: Long
    val allocationSize: Long
    val isCoherent: Boolean

    init {
        // We create a container to get values out of the stack scope
        val result = memoryStack { stack ->
            val bufferInfo = VkBufferCreateInfo.calloc(stack).`sType$Default`()
                .size(size)
                .usage(usage)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE)

            val bHandle = stack.createHandle({ "Buffer Creation Failed" }) {
                vkCreateBuffer(device, bufferInfo, null, it)
            }

            val memReqs = VkMemoryRequirements.calloc(stack)
            vkGetBufferMemoryRequirements(device, bHandle, memReqs)

            val typeIndex = physicalDevice.memoryTypeFromProperties(memReqs.memoryTypeBits(), properties)

            // Check coherency by looking at the memory type's flags
            val coherent = (physicalDevice.memoryProperties.memoryTypes(typeIndex).propertyFlags() and VK_MEMORY_PROPERTY_HOST_COHERENT_BIT) != 0

            val allocInfo = VkMemoryAllocateInfo.calloc(stack).`sType$Default`()
                .allocationSize(memReqs.size())
                .memoryTypeIndex(typeIndex)

            val mHandle = stack.createHandle({ "Memory Allocation Failed" }) {
                vkAllocateMemory(device, allocInfo, null, it)
            }

            vkCheck(vkBindBufferMemory(device, bHandle, mHandle, 0))

            // Return a simple data holder or a Triple/Quad
            AllocationResult(bHandle, mHandle, memReqs.size(), coherent)
        }

        this.handle = result.bufferHandle
        this.memory = result.memoryHandle
        this.allocationSize = result.allocationSize
        this.isCoherent = result.isCoherent
    }

    private data class AllocationResult(val bufferHandle: Long, val memoryHandle: Long, val allocationSize: Long, val isCoherent: Boolean)

    var mappedMemory: Long = NULL
        private set

    fun map(offset: Long = 0, mapSize: Long = size): Long {
        if (mappedMemory == NULL) {
            memoryStack { stack ->
                mappedMemory = stack.createPointer({ "Failed to map Buffer" }) {
                    vkMapMemory(device, memory, offset, mapSize, 0, it)
                }
            }
        }

        return mappedMemory
    }

    fun flush(offset: Long = 0, flushSize: Long = size) {
        if (isCoherent) return

        // If the memory isn't coherent, we MUST manually tell the GPU we updated it.
        memoryStack { stack ->
            val range = VkMappedMemoryRange.calloc(1, stack).`sType$Default`()
                .memory(memory)
                .offset(offset)
                .size(flushSize)
            vkFlushMappedMemoryRanges(device, range)
        }
    }

    fun unmap() {
        if (mappedMemory == NULL) return
        vkUnmapMemory(device, memory)
        mappedMemory = NULL
    }

    override fun destroy() {
        unmap()
        vkDestroyBuffer(device, handle, null)
        vkFreeMemory(device, memory, null)
    }
}