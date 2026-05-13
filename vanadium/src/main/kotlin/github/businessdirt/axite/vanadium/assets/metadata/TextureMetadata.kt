package github.businessdirt.axite.vanadium.assets.metadata

import kotlinx.serialization.Serializable
import org.lwjgl.vulkan.VK10.VK_FILTER_LINEAR
import org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT
import java.util.*

@Serializable
data class TextureMetadata(
    override val uuid: String = UUID.randomUUID().toString(),
    override val version: Int = 1,
    val width: Int = 0,
    val height: Int = 0,
    val format: Int = 0,
    val mipLevels: Int = 1,
    val minFilter: Int = VK_FILTER_LINEAR,
    val magFilter: Int = VK_FILTER_LINEAR,
    val addressModeU: Int = VK_SAMPLER_ADDRESS_MODE_REPEAT,
    val addressModeV: Int = 0,
    val addressModeW: Int = 0
) : AssetMetadata()
