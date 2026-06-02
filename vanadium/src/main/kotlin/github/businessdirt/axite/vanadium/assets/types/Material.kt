package github.businessdirt.axite.vanadium.assets.types

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.assets.metadata.MaterialMetadata
import org.joml.Vector4f

class Material(
    path: String,
    uuid: String,
    metadata: MaterialMetadata,
    albedoTexture: Texture?,
    normalTexture: Texture?,
    metallicRoughnessTexture: Texture?,
    emissiveTexture: Texture?,
    baseColor: Vector4f = Vector4f(1f, 1f, 1f, 1f),
    metallicFactor: Float = 1.0f,
    roughnessFactor: Float = 1.0f,
    isTransparent: Boolean
) : Asset<Material>(uuid, path, metadata) {

    override var metadata: MaterialMetadata = metadata
        private set

    var albedoTexture: Texture? = albedoTexture
        private set

    var normalTexture: Texture? = normalTexture
        private set

    var metallicRoughnessTexture: Texture? = metallicRoughnessTexture
        private set

    var emissiveTexture: Texture? = emissiveTexture
        private set

    var baseColor: Vector4f = baseColor
        private set

    var metallicFactor: Float = metallicFactor
        private set

    var roughnessFactor: Float = roughnessFactor
        private set

    var isTransparent: Boolean = isTransparent
        private set

    override fun update(newAsset: Material) {
        val oldAlbedo = this.albedoTexture
        val oldNormal = this.normalTexture
        val oldMetallicRoughness = this.metallicRoughnessTexture
        val oldEmissive = this.emissiveTexture

        this.metadata = newAsset.metadata
        this.albedoTexture = newAsset.albedoTexture
        this.normalTexture = newAsset.normalTexture
        this.metallicRoughnessTexture = newAsset.metallicRoughnessTexture
        this.emissiveTexture = newAsset.emissiveTexture
        this.baseColor = newAsset.baseColor
        this.metallicFactor = newAsset.metallicFactor
        this.roughnessFactor = newAsset.roughnessFactor
        this.isTransparent = newAsset.isTransparent

        disposeInternal(oldAlbedo, oldNormal, oldMetallicRoughness, oldEmissive)
    }

    private fun disposeInternal(
        albedo: Texture?,
        normal: Texture?,
        metallicRoughness: Texture?,
        emissive: Texture?
    ) {
        albedo?.let { Vanadium.assets.unload(it.path) }
        normal?.let { Vanadium.assets.unload(it.path) }
        metallicRoughness?.let { Vanadium.assets.unload(it.path) }
        emissive?.let { Vanadium.assets.unload(it.path) }
    }

    override fun dispose() = disposeInternal(albedoTexture, normalTexture, metallicRoughnessTexture, emissiveTexture)
}
