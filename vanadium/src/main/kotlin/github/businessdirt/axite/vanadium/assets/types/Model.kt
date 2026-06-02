package github.businessdirt.axite.vanadium.assets.types

import github.businessdirt.axite.vanadium.assets.metadata.ModelMetadata
import github.businessdirt.axite.vanadium.scene.Mesh

class Model(
    path: String,
    uuid: String,
    metadata: ModelMetadata,
    meshes: List<Mesh>,
    materials: List<Material> = emptyList()
) : Asset<Model>(uuid, path, metadata) {

    override var metadata: ModelMetadata = metadata
        private set

    var meshes: List<Mesh> = meshes
        private set

    var materials: List<Material> = materials
        private set

    override fun update(newAsset: Model) {
        val oldMeshes = this.meshes
        val oldMaterials = this.materials

        this.metadata = newAsset.metadata
        this.meshes = newAsset.meshes
        this.materials = newAsset.materials

        oldMeshes.forEach { mesh -> mesh.close() }
        oldMaterials.forEach { material -> material.release() }
    }

    override fun dispose() {
        meshes.forEach { mesh -> mesh.close() }
        materials.forEach { material -> material.release() }
    }
}
