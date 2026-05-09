package github.businessdirt.axite.vanadium.vulkan.resources

import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.Handle
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkImageViewCreateInfo

class ImageView(
    private val device: VkDevice,
    val imageHandle: Long,
    block: Data.() -> Unit
) : Handle<Long>() {

    val data = Data().apply(block)

    val aspectMask: Int = data.aspectMask
    val mipLevels: Int = data.mipLevels

    override val handle: Long = memoryStack { stack ->
        val viewCreateInfo = VkImageViewCreateInfo.calloc(stack).`sType$Default`()
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
            vkCreateImageView(device, viewCreateInfo, null, longBuffer)
        }
    }

    override fun destroy() = vkDestroyImageView(device, handle, null)

    class Data {
        var aspectMask: Int = 0
        var baseArrayLayer: Int = 0
        var format: Int = 0
        var layerCount: Int = 1
        var mipLevels: Int = 1
        var viewType: Int = VK_IMAGE_VIEW_TYPE_2D
    }
}