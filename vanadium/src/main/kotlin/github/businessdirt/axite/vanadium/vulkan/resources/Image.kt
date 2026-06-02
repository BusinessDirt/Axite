package github.businessdirt.axite.vanadium.vulkan.resources

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.core.utils.vkCheck
import github.businessdirt.axite.vanadium.vulkan.Handle
import github.businessdirt.axite.vanadium.vulkan.device.PhysicalDevice
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*

class Image(
    private val device: VkDevice,
    private val physicalDevice: PhysicalDevice,
    existingHandle: Long? = null,
    block: Data.() -> Unit
) : Handle<Long>() {

    val data = Data().apply(block)

    val format: Int = data.format
    val mipLevels: Int = data.mipLevels

    private var allocation: Long = 0

    override var handle: Long = existingHandle ?: memoryStack { stack ->

        val imageCreateInfo = VkImageCreateInfo.calloc(stack).`sType$Default`()
            .imageType(VK_IMAGE_TYPE_2D)
            .format(format)
            .extent { it.width(data.width).height(data.height).depth(1) }
            .mipLevels(mipLevels)
            .arrayLayers(data.arrayLayers)
            .samples(data.sampleCount)
            .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            .tiling(VK_IMAGE_TILING_OPTIMAL)
            .usage(data.usage)

        val allocInfo = VmaAllocationCreateInfo.calloc(stack)
            .usage(VMA_MEMORY_USAGE_AUTO)
            .requiredFlags(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)

        val pImage = stack.mallocLong(1)
        val pAllocation = stack.mallocPointer(1)

        vkCheck(vmaCreateImage(Vanadium.context.memoryAllocator.handle, imageCreateInfo, allocInfo, pImage, pAllocation, null)) {
            "Failed to create VMA image"
        }

        allocation = pAllocation[0]
        pImage[0]
    }

    override fun destroy() {
        if (allocation != 0L) {
            vmaDestroyImage(Vanadium.context.memoryAllocator.handle, handle, allocation)
        }
    }

    class Data(
        var width: Int = 0,
        var height: Int = 0,
        var usage: Int = 0,
        var memoryUsage: Int = 0,
        var format: Int = VK_FORMAT_R8G8B8A8_SRGB,
        var mipLevels: Int = 1,
        var sampleCount: Int = VK_SAMPLE_COUNT_1_BIT,
        var arrayLayers: Int = 1,
    )
}