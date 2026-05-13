package github.businessdirt.axite.vanadium.vulkan.resources

import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.Handle
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkSamplerCreateInfo

class Sampler(
    private val device: VkDevice,
    block: Data.() -> Unit
) : Handle<Long>() {

    val data = Data().apply(block)

    override val handle: Long = memoryStack { stack ->
        val samplerInfo = VkSamplerCreateInfo.calloc(stack).`sType$Default`()
            .magFilter(data.magFilter)
            .minFilter(data.minFilter)
            .addressModeU(data.addressModeU)
            .addressModeV(data.addressModeV)
            .addressModeW(data.addressModeW)
            .anisotropyEnable(data.anisotropyEnable)
            .maxAnisotropy(data.maxAnisotropy)
            .borderColor(data.borderColor)
            .unnormalizedCoordinates(data.unnormalizedCoordinates)
            .compareEnable(data.compareEnable)
            .compareOp(data.compareOp)
            .mipmapMode(data.mipmapMode)
            .mipLodBias(data.mipLodBias)
            .minLod(data.minLod)
            .maxLod(data.maxLod)

        stack.createHandle({ "Failed to create sampler" }) {
            vkCreateSampler(device, samplerInfo, null, it)
        }
    }

    override fun destroy() = vkDestroySampler(device, handle, null)

    class Data(
        var magFilter: Int = VK_FILTER_LINEAR,
        var minFilter: Int = VK_FILTER_LINEAR,
        var addressModeU: Int = VK_SAMPLER_ADDRESS_MODE_REPEAT,
        var addressModeV: Int = VK_SAMPLER_ADDRESS_MODE_REPEAT,
        var addressModeW: Int = VK_SAMPLER_ADDRESS_MODE_REPEAT,
        var anisotropyEnable: Boolean = true,
        var maxAnisotropy: Float = 16f,
        var borderColor: Int = VK_BORDER_COLOR_INT_OPAQUE_BLACK,
        var unnormalizedCoordinates: Boolean = false,
        var compareEnable: Boolean = false,
        var compareOp: Int = VK_COMPARE_OP_ALWAYS,
        var mipmapMode: Int = VK_SAMPLER_MIPMAP_MODE_LINEAR,
        var mipLodBias: Float = 0f,
        var minLod: Float = 0f,
        var maxLod: Float = 1f
    )
}
