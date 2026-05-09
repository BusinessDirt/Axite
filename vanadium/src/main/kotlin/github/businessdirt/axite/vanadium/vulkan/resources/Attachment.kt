package github.businessdirt.axite.vanadium.vulkan.resources

import github.businessdirt.axite.vanadium.vulkan.Handle
import github.businessdirt.axite.vanadium.vulkan.device.Device
import github.businessdirt.axite.vanadium.vulkan.device.PhysicalDevice
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkDevice

class Attachment(
    private val device: VkDevice,
    physicalDevice: PhysicalDevice,
    val width: Int,
    val height: Int,
    val format: Int,
    val usage: Int,
    existingImage: Image? = null
) : Handle<Long>() {

    val isDepth: Boolean = isDepthFormat(format)
    val isStencil: Boolean = isStencilFormat(format)

    val image: Image = existingImage ?: Image(device, physicalDevice) {
        this.width = this@Attachment.width
        this.height = this@Attachment.height
        this.format = this@Attachment.format
        this.usage = this@Attachment.usage or VK_IMAGE_USAGE_SAMPLED_BIT
    }

    override val handle: Long get() = image.handle

    fun updateImageHandle(newHandle: Long) {
        if (image.handle == newHandle) return
        image.handle = newHandle
        
        // Recreate image view
        imageView.close()
        imageView = ImageView(device, image.handle) {
            this.format = image.format
            this.aspectMask = this@Attachment.aspectMask
        }
    }

    val aspectMask: Int = run {
        var mask = 0
        if (isDepth) mask = mask or VK_IMAGE_ASPECT_DEPTH_BIT
        if (isStencil) mask = mask or VK_IMAGE_ASPECT_STENCIL_BIT
        if (!isDepth && !isStencil) mask = VK_IMAGE_ASPECT_COLOR_BIT
        mask
    }

    var imageView: ImageView = ImageView(device, image.handle) {
        this.format = image.format
        this.aspectMask = this@Attachment.aspectMask
    }

    /**
     * TRACKING: Crucial for Dynamic Rendering.
     * Keeps track of what the image is currently doing to automate barriers.
     */
    var currentLayout: Int = VK_IMAGE_LAYOUT_UNDEFINED
        internal set

    override fun destroy() {
        imageView.close()
        image.close()
    }

    companion object {
        fun isDepthFormat(format: Int): Boolean = format in listOf(
            VK_FORMAT_D16_UNORM, VK_FORMAT_D32_SFLOAT,
            VK_FORMAT_D16_UNORM_S8_UINT, VK_FORMAT_D24_UNORM_S8_UINT, VK_FORMAT_D32_SFLOAT_S8_UINT
        )

        fun isStencilFormat(format: Int): Boolean = format in listOf(
            VK_FORMAT_S8_UINT, VK_FORMAT_D16_UNORM_S8_UINT,
            VK_FORMAT_D24_UNORM_S8_UINT, VK_FORMAT_D32_SFLOAT_S8_UINT
        )
    }
}