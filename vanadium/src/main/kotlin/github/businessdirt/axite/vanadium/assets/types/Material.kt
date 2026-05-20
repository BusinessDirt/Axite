package github.businessdirt.axite.vanadium.assets.types

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
        this.metadata = newAsset.metadata
        this.albedoTexture = newAsset.albedoTexture
        this.normalTexture = newAsset.normalTexture
        this.metallicRoughnessTexture = newAsset.metallicRoughnessTexture
        this.emissiveTexture = newAsset.emissiveTexture
        this.baseColor = newAsset.baseColor
        this.metallicFactor = newAsset.metallicFactor
        this.roughnessFactor = newAsset.roughnessFactor
        this.isTransparent = newAsset.isTransparent
    }

    override fun dispose() { }
}
