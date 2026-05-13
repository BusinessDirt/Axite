package github.businessdirt.axite.vanadium.assets.types

import github.businessdirt.axite.vanadium.assets.metadata.ModelMetadata
import github.businessdirt.axite.vanadium.scene.Mesh

class Model(
    override val path: String,
    override val uuid: String,
    override val metadata: ModelMetadata,
    val meshes: List<Mesh>
) : Asset(uuid, path, metadata) {
    override fun dispose() = meshes.forEach { it.close() }
}
