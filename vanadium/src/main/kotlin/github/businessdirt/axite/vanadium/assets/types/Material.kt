package github.businessdirt.axite.vanadium.assets.types

import github.businessdirt.axite.vanadium.assets.metadata.MaterialMetadata
import org.joml.Vector4f

class Material(
    override val path: String,
    override val uuid: String,
    override val metadata: MaterialMetadata,
    val albedoTexture: Texture?,
    val normalTexture: Texture?,
    val metallicRoughnessTexture: Texture?,
    val emissiveTexture: Texture?,
    val baseColor: Vector4f = Vector4f(1f, 1f, 1f, 1f),
    val metallicFactor: Float = 1.0f,
    val roughnessFactor: Float = 1.0f
) : Asset {
    override fun release() { }
}
