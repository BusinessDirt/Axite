package github.businessdirt.axite.vanadium.assets.types

import github.businessdirt.axite.vanadium.assets.metadata.ModelMetadata
import github.businessdirt.axite.vanadium.renderer.scene.Mesh

class Model(
    override val path: String,
    override val uuid: String,
    override val metadata: ModelMetadata,
    val meshes: List<Mesh>
) : Asset {
    override fun release() = meshes.forEach { it.close() }
}
