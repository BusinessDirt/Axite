package github.businessdirt.axite.vanadium.vulkan.resources

import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.findMemoryTypeIndex
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.core.utils.vkCheck
import github.businessdirt.axite.vanadium.vulkan.Handle
import github.businessdirt.axite.vanadium.vulkan.device.PhysicalDevice
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkImageCreateInfo
import org.lwjgl.vulkan.VkMemoryAllocateInfo
import org.lwjgl.vulkan.VkMemoryRequirements

class Image(
    private val device: VkDevice,
    physicalDevice: PhysicalDevice,
    block: Data.() -> Unit
) : Handle<Long>() {

    private val data = Data().apply(block)

    val format: Int = data.format
    val mipLevels: Int = data.mipLevels

    override val handle: Long = memoryStack { stack ->
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

        stack.createHandle({ "Failed to create image" }) {
            vkCreateImage(device, imageCreateInfo, null, it)
        }
    }

    val memoryHandle: Long = memoryStack { stack ->
        // Get memory requirements for this object
        val memoryRequirements = VkMemoryRequirements.calloc(stack)
        vkGetImageMemoryRequirements(device, handle, memoryRequirements)

        // Select memory size and type
        val memoryAllocationInfo = VkMemoryAllocateInfo.calloc(stack).`sType$Default`()
            .allocationSize(memoryRequirements.size())
            .findMemoryTypeIndex(physicalDevice, memoryRequirements.memoryTypeBits(), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)


        // Allocate and bind memory
        stack.createHandle({ "Failed to allocate memory" }) {
            vkAllocateMemory(device, memoryAllocationInfo, null, it)
        }.also {
            vkCheck(vkBindImageMemory(device, handle, it, 0)) {
                "Failed to bind image memory"
            }
        }
    }

    override fun destroy() {
        vkDestroyImage(device, handle, null);
        vkFreeMemory(device, memoryHandle, null);
    }

    class Data(
        var width: Int = 0,
        var height: Int = 0,
        var usage: Int = 0,
        var format: Int = VK_FORMAT_R8G8B8A8_SRGB,
        var mipLevels: Int = 1,
        var sampleCount: Int = VK_SAMPLE_COUNT_1_BIT,
        var arrayLayers: Int = 1,
    )
}