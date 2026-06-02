package github.businessdirt.axite.vanadium.vulkan.resources

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.core.utils.vkCheck
import github.businessdirt.axite.vanadium.vulkan.Handle
import github.businessdirt.axite.vanadium.vulkan.device.PhysicalDevice
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocationCreateInfo
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
    private val allocation: Long

    init {
        val result = memoryStack { stack ->
            val bufferInfo = VkBufferCreateInfo.calloc(stack).`sType$Default`()
                .size(size)
                .usage(usage)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE)

            val allocInfo = VmaAllocationCreateInfo.calloc(stack)
                .usage(VMA_MEMORY_USAGE_AUTO)
                .requiredFlags(properties)

            if ((properties and VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0) {
                allocInfo.flags(VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT or VMA_ALLOCATION_CREATE_MAPPED_BIT)
            }

            val pBuffer = stack.mallocLong(1)
            val pAllocation = stack.mallocPointer(1)

            vkCheck(vmaCreateBuffer(Vanadium.context.memoryAllocator.handle, bufferInfo, allocInfo, pBuffer, pAllocation, null)) {
                "Failed to create VMA buffer"
            }

            pBuffer[0] to pAllocation[0]
        }

        this.handle = result.first
        this.allocation = result.second
    }

    var mappedMemory: Long = NULL
        private set

    fun map(): Long {
        if (mappedMemory == NULL) {
            memoryStack { stack ->
                val pMapped = stack.mallocPointer(1)
                vkCheck(vmaMapMemory(Vanadium.context.memoryAllocator.handle, allocation, pMapped)) {
                    "Failed to map VMA memory"
                }
                mappedMemory = pMapped[0]
            }
        }

        return mappedMemory
    }

    fun unmap() {
        if (mappedMemory == NULL) return
        vmaUnmapMemory(Vanadium.context.memoryAllocator.handle, allocation)
        mappedMemory = NULL
    }

    override fun destroy() {
        unmap()
        vmaDestroyBuffer(Vanadium.context.memoryAllocator.handle, handle, allocation)
    }
}