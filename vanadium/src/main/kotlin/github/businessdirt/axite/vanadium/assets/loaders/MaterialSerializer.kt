package github.businessdirt.axite.vanadium.assets.loaders

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.assets.metadata.MaterialMetadata
import github.businessdirt.axite.vanadium.assets.types.Material
import github.businessdirt.axite.vanadium.assets.types.Texture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joml.Vector4f
import java.io.File

class MaterialSerializer : AssetSerializer<Material, MaterialMetadata>(
    MaterialMetadata.serializer()
) {

    override suspend fun load(path: String): Material = withContext(Dispatchers.IO) {
        val file = File(path)
        val metadata = if (file.exists() && path.endsWith(".meta")) {
            json.decodeFromString<MaterialMetadata>(file.readText())
        } else {
            loadMetadata(path) ?: MaterialMetadata()
        }

        if (!hasMetadata(path)) Vanadium.engineScope.launch(Dispatchers.IO) {
            writeMetadata(path, metadata)
        }

        val albedo = metadata.albedoPath?.let { Vanadium.assets.load<Texture>(it) }
        val normal = metadata.normalPath?.let { Vanadium.assets.load<Texture>(it) }
        val metallicRoughness = metadata.metallicRoughnessPath?.let { Vanadium.assets.load<Texture>(it) }
        val emissive = metadata.emissivePath?.let { Vanadium.assets.load<Texture>(it) }

        Material(
            path = path,
            uuid = metadata.uuid,
            metadata = metadata,
            albedoTexture = albedo,
            normalTexture = normal,
            metallicRoughnessTexture = metallicRoughness,
            emissiveTexture = emissive,
            baseColor = Vector4f(metadata.baseColorR, metadata.baseColorG, metadata.baseColorB, metadata.baseColorA),
            metallicFactor = metadata.metallicFactor,
            roughnessFactor = metadata.roughnessFactor,
            isTransparent = metadata.isTransparent || (albedo?.isTransparent ?: false)
        )
    }
}
