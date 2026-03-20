package github.businessdirt.axite.vanadium.platform.vulkan.resources

import org.lwjgl.vulkan.VK13.*

class Attachment(
    width: Int, height: Int,
    format: Int, usage: Int,
) {

    val isDepthAttachment: Boolean = (usage and VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT) != 0

    val image: Image = Image {
        this.width = width
        this.height = height
        this.format = format
        this.usage = usage or VK_IMAGE_USAGE_SAMPLED_BIT
    }

    val imageView: ImageView = ImageView(image.handle) {
        this.format = image.format
        this.aspectMask = (if (isDepthAttachment) VK_IMAGE_ASPECT_DEPTH_BIT else VK_IMAGE_ASPECT_COLOR_BIT)
    }

    fun cleanup() {
        imageView.cleanup()
        image.cleanup()
    }
}