package github.businessdirt.axite.vanadium.assets.types

import github.businessdirt.axite.vanadium.assets.metadata.TextureMetadata
import github.businessdirt.axite.vanadium.vulkan.resources.Image
import github.businessdirt.axite.vanadium.vulkan.resources.ImageView
import github.businessdirt.axite.vanadium.vulkan.resources.Sampler

class Texture(
    override val path: String,
    override val uuid: String,
    override val metadata: TextureMetadata,
    val image: Image,
    val view: ImageView,
    val sampler: Sampler
) : Asset {
    override fun release() {
        sampler.close()
        view.close()
        image.close()
    }
}
