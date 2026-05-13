package github.businessdirt.axite.vanadium.assets.types

import github.businessdirt.axite.vanadium.assets.metadata.ModelMetadata
import github.businessdirt.axite.vanadium.scene.Mesh

class Model(
    path: String,
    uuid: String,
    metadata: ModelMetadata,
    meshes: List<Mesh>
) : Asset(uuid, path, metadata) {

    override var metadata: ModelMetadata = metadata
        private set

    var meshes: List<Mesh> = meshes
        private set

    override fun update(newAsset: Asset) {
        if (newAsset is Model) {
            val oldMeshes = this.meshes
            this.metadata = newAsset.metadata
            this.meshes = newAsset.meshes
            oldMeshes.forEach { it.close() }
        }
    }

    override fun dispose() = meshes.forEach { it.close() }
}
