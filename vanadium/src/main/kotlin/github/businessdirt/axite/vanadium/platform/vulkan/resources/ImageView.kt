package github.businessdirt.axite.vanadium.platform.vulkan.resources

import github.businessdirt.axite.vanadium.platform.vulkan.Context
import github.businessdirt.axite.vanadium.platform.vulkan.VulkanHandle
import github.businessdirt.axite.vanadium.utils.createHandle
import github.businessdirt.axite.vanadium.utils.memoryStack
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkImageViewCreateInfo

class ImageView(
    val imageHandle: Long,
    block: ImageViewData.() -> Unit
) : VulkanHandle<Long>() {

    private val data = ImageViewData().apply(block)

    val aspectMask: Int = data.aspectMask
    val mipLevels: Int = data.mipLevels

    override val handle: Long = memoryStack { stack ->
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

        stack.createHandle({ "Failed to create image view" }) { longBuffer ->
            vkCreateImageView(Context.device.handle, viewCreateInfo, null, longBuffer)
        }
    }

    override fun destroy() = vkDestroyImageView(Context.device.handle, handle, null)

    class ImageViewData {
        var aspectMask: Int = 0
        var baseArrayLayer: Int = 0
        var format: Int = 0
        var layerCount: Int = 1
        var mipLevels: Int = 1
        var viewType: Int = VK_IMAGE_VIEW_TYPE_2D
    }
}