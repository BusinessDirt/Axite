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
import kotlinx.coroutines.withContext
import kotlinx.serialization.serializer
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkBufferImageCopy2
import org.lwjgl.vulkan.VkCopyBufferToImageInfo2
import java.io.File

class TextureSerializer : AssetSerializer<Texture, TextureMetadata>(
    serializer<TextureMetadata>(),
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
            stbi_image_free(pixels)

            val format = if (metadata.format == 0) VK_FORMAT_R8G8B8A8_SRGB else metadata.format
            val image = Image(Vanadium.context.device.handle, Vanadium.context.physicalDevice) {
                this.width = width
                this.height = height
                this.format = format
                this.usage = VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT
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

                    stack.imageBarrier(
                        handle, image.handle,
                        VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                        VK_PIPELINE_STAGE_2_TRANSFER_BIT, VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT,
                        VK_ACCESS_2_TRANSFER_WRITE_BIT, VK_ACCESS_2_SHADER_READ_BIT,
                        VK_IMAGE_ASPECT_COLOR_BIT
                    )
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
                format = format
            )

            Texture(path, finalMetadata.uuid, finalMetadata, image, view, sampler)
        }
    }
}
