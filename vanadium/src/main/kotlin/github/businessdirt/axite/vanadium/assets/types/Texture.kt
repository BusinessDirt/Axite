package github.businessdirt.axite.vanadium.assets.types

import github.businessdirt.axite.vanadium.assets.metadata.TextureMetadata
import github.businessdirt.axite.vanadium.vulkan.resources.Image
import github.businessdirt.axite.vanadium.vulkan.resources.ImageView
import github.businessdirt.axite.vanadium.vulkan.resources.Sampler
import java.nio.ByteBuffer

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

    var isTransparent: Boolean = false
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

    fun setTransparent(data: ByteBuffer) {
        val numPixels = data.capacity() / 4
        var offset = 0

        isTransparent = false

        for (i in 0..<numPixels) {
            val a = (0xFF and data.get(offset + 3).toInt())

            if (a < 255) {
                isTransparent = true
                break
            }

            offset += 4
        }
    }

    override fun dispose() {
        sampler.close()
        view.close()
        image.close()
    }
}
