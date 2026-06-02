package github.businessdirt.axite.vanadium.assets.loaders

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.assets.metadata.TextureMetadata
import github.businessdirt.axite.vanadium.assets.types.Texture
import github.businessdirt.axite.vanadium.core.utils.imageBarrier
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.resources.Buffer
import github.businessdirt.axite.vanadium.vulkan.resources.Image
import github.businessdirt.axite.vanadium.vulkan.resources.ImageView
import github.businessdirt.axite.vanadium.vulkan.resources.Sampler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*
import java.io.File
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.min


class TextureSerializer : AssetSerializer<Texture, TextureMetadata>(
    TextureMetadata.serializer()
) {

    override suspend fun load(path: String): Texture = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) throw IllegalArgumentException("Texture file not found: $path")

        val metadata = loadMetadata(path) ?: TextureMetadata()

        memoryStack { stack ->
            val pWidth = stack.mallocInt(1)
            val pHeight = stack.mallocInt(1)
            val pChannels = stack.mallocInt(1)

            val pixels = stbi_load(path, pWidth, pHeight, pChannels, STBI_rgb_alpha)
                ?: throw RuntimeException("Failed to load texture image: [${stbi_failure_reason()}]")

            val width = pWidth[0]
            val height = pHeight[0]
            val imageSize = (width * height * 4).toLong()

            val stagingBuffer = Buffer(
                Vanadium.context.device.handle,
                Vanadium.context.physicalDevice,
                imageSize,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
            )

            val pStaging = stagingBuffer.map()
            MemoryUtil.memCopy(MemoryUtil.memAddress(pixels), pStaging, imageSize)
            stagingBuffer.unmap()

            val format = if (metadata.format == 0) VK_FORMAT_R8G8B8A8_SRGB else metadata.format
            val image = Image(Vanadium.context.device.handle, Vanadium.context.physicalDevice) {
                this.width = width
                this.height = height
                this.format = format
                this.usage = VK_IMAGE_USAGE_TRANSFER_SRC_BIT or VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT
                this.mipLevels = metadata.mipLevels
            }

            // Copy buffer to image
            Vanadium.context.graphicsQueue.execute {
                memoryStack { stack ->
                    stack.imageBarrier(
                        handle, image.handle,
                        VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_2_TRANSFER_BIT,
                        0, VK_ACCESS_2_TRANSFER_WRITE_BIT,
                        VK_IMAGE_ASPECT_COLOR_BIT
                    )

                    val copyRegion = VkBufferImageCopy2.calloc(1, stack).`sType$Default`()
                        .imageSubresource { it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).mipLevel(0).baseArrayLayer(0).layerCount(1) }
                        .imageExtent { it.set(width, height, 1) }

                    val copyInfo = VkCopyBufferToImageInfo2.calloc(stack).`sType$Default`()
                        .srcBuffer(stagingBuffer.handle)
                        .dstImage(image.handle)
                        .dstImageLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                        .pRegions(copyRegion)

                    vkCmdCopyBufferToImage2(handle, copyInfo)

                    handle.generateMipMaps(stack, width, height, image)
                }
            }

            stagingBuffer.close()

            val view = ImageView(Vanadium.context.device.handle, image.handle) {
                this.format = format
                this.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT
            }

            val sampler = Sampler(Vanadium.context.device.handle) {
                this.magFilter = metadata.magFilter
                this.minFilter = metadata.minFilter
                this.addressModeU = metadata.addressModeU
                this.addressModeV = metadata.addressModeV
                this.addressModeW = metadata.addressModeW
            }

            val finalMetadata = metadata.copy(
                width = width,
                height = height,
                format = format,
                mipLevels = floor(log2(min(width, height).toDouble())).toInt() + 1
            )

            if (!hasMetadata(path)) Vanadium.engineScope.launch(Dispatchers.IO) {
                writeMetadata(path, finalMetadata)
            }

            Texture(path, finalMetadata.uuid, finalMetadata, image, view, sampler).apply {
                setTransparent(pixels)
                stbi_image_free(pixels)
            }
        }
    }

    private fun VkCommandBuffer.generateMipMaps(
        stack: MemoryStack,
        width: Int, height: Int,
        image: Image,
    ) {
        val subResourceRange = VkImageSubresourceRange.calloc(stack)
            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
            .baseArrayLayer(0)
            .levelCount(1)
            .layerCount(1)

        val barrier = VkImageMemoryBarrier2.calloc(1, stack).`sType$Default`()
            .image(image.handle)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .subresourceRange(subResourceRange)

        val depInfo = VkDependencyInfo.calloc(stack).`sType$Default`()
            .pImageMemoryBarriers(barrier)

        var mipWidth = width
        var mipHeight = height

        val mipLevels: Int = image.mipLevels
        for (i in 1..<mipLevels) {
            subResourceRange.baseMipLevel(i - 1)
            barrier.subresourceRange(subResourceRange)
                .oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                .newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                .srcStageMask(VK_PIPELINE_STAGE_TRANSFER_BIT.toLong())
                .dstStageMask(VK_PIPELINE_STAGE_TRANSFER_BIT.toLong())
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT.toLong())
                .dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT.toLong())

            vkCmdPipelineBarrier2(this, depInfo)

            val srcOffset0 = VkOffset3D.calloc(stack).x(0).y(0).z(0)
            val srcOffset1 = VkOffset3D.calloc(stack).x(mipWidth).y(mipHeight).z(1)

            val dstOffset0 = VkOffset3D.calloc(stack).x(0).y(0).z(0)
            val dstOffset1 = VkOffset3D.calloc(stack).x(mipWidth.clampMip()).y(mipHeight.clampMip()).z(1)

            val blit = VkImageBlit.calloc(1, stack)
                .srcOffsets(0, srcOffset0)
                .srcOffsets(1, srcOffset1)
                .srcSubresource {
                    it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .mipLevel(i - 1)
                        .baseArrayLayer(0)
                        .layerCount(1)
                }
                .dstOffsets(0, dstOffset0)
                .dstOffsets(1, dstOffset1)
                .dstSubresource {
                    it.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .mipLevel(i)
                        .baseArrayLayer(0)
                        .layerCount(1)
                }

            vkCmdBlitImage(
                this,
                image.handle, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                image.handle, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                blit, VK_FILTER_LINEAR
            )

            barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                .newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                .srcAccessMask(VK_ACCESS_TRANSFER_READ_BIT.toLong())
                .dstAccessMask(VK_ACCESS_SHADER_READ_BIT.toLong())

            barrier.srcStageMask(VK_PIPELINE_STAGE_TRANSFER_BIT.toLong())
                .dstStageMask(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT.toLong())

            vkCmdPipelineBarrier2(this, depInfo)

            if (mipWidth > 1) mipWidth /= 2
            if (mipHeight > 1) mipHeight /= 2
        }

        barrier.subresourceRange { it.baseMipLevel(mipLevels - 1) }
            .oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
            .newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
            .srcStageMask(VK_PIPELINE_STAGE_TRANSFER_BIT.toLong())
            .dstStageMask(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT.toLong())
            .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT.toLong())
            .dstAccessMask(VK_ACCESS_SHADER_READ_BIT.toLong())

        vkCmdPipelineBarrier2(this, depInfo)
    }

    private fun Int.clampMip() = if (this > 1) this / 2 else 1
}
