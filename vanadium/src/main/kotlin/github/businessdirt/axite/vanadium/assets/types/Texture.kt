package github.businessdirt.axite.vanadium.assets.types

import github.businessdirt.axite.vanadium.assets.metadata.TextureMetadata
import github.businessdirt.axite.vanadium.vulkan.resources.Image
import github.businessdirt.axite.vanadium.vulkan.resources.ImageView
import github.businessdirt.axite.vanadium.vulkan.resources.Sampler

class Texture(
    path: String,
    uuid: String,
    metadata: TextureMetadata,
    image: Image,
    view: ImageView,
    sampler: Sampler
) : Asset<Texture>(uuid, path, metadata) {

    override var metadata: TextureMetadata = metadata
        private set

    var image: Image = image
        private set
    var view: ImageView = view
        private set
    var sampler: Sampler = sampler
        private set

    override fun update(newAsset: Texture) {
        val oldImage = this.image
        val oldView = this.view
        val oldSampler = this.sampler

        this.metadata = newAsset.metadata
        this.image = newAsset.image
        this.view = newAsset.view
        this.sampler = newAsset.sampler

        oldSampler.close()
        oldView.close()
        oldImage.close()
    }

    override fun dispose() {
        sampler.close()
        view.close()
        image.close()
    }
}
