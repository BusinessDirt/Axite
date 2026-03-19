package github.businessdirt.axite.vanadium.platform.vulkan.resources

import github.businessdirt.axite.vanadium.platform.vulkan.Device
import github.businessdirt.axite.vanadium.platform.vulkan.VulkanHandle
import github.businessdirt.axite.vanadium.utils.memoryStack
import github.businessdirt.axite.vanadium.utils.vkCheck
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkImageViewCreateInfo

class ImageView(
    val device: Device,
    val imageHandle: Long,
    block: ImageViewData.() -> Unit
) : VulkanHandle<Long>() {

    private val data = ImageViewData().apply(block)

    val aspectMask: Int = data.aspectMask
    val mipLevels: Int = data.mipLevels

    override val handle: Long = memoryStack { stack ->
        val lp = stack.mallocLong(1)
        val viewCreateInfo = VkImageViewCreateInfo.calloc(stack)
            .`sType$Default`()
            .image(imageHandle)
            .viewType(data.viewType)
            .format(data.format)
            .subresourceRange {
                it.aspectMask(aspectMask)
                    .baseMipLevel(0)
                    .levelCount(mipLevels)
                    .baseArrayLayer(data.baseArrayLayer)
                    .layerCount(data.layerCount)
            }

        vkCheck(vkCreateImageView(device.handle, viewCreateInfo, null, lp)) {
            "Failed to create image view"
        }

        lp[0]
    }

    override fun destroy() = vkDestroyImageView(device.handle, handle, null)

    class ImageViewData {
        var aspectMask: Int = 0
        var baseArrayLayer: Int = 0
        var format: Int = 0
        var layerCount: Int = 1
        var mipLevels: Int = 1
        var viewType: Int = VK_IMAGE_VIEW_TYPE_2D
    }
}