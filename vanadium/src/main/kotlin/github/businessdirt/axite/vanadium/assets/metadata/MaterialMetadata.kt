package github.businessdirt.axite.vanadium.assets.metadata

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class MaterialMetadata(
    override val uuid: String = UUID.randomUUID().toString(),
    override val version: Int = 1,
    val albedoPath: String? = null,
    val normalPath: String? = null,
    val metallicRoughnessPath: String? = null,
    val emissivePath: String? = null,
    val baseColorR: Float = 1.0f,
    val baseColorG: Float = 1.0f,
    val baseColorB: Float = 1.0f,
    val baseColorA: Float = 1.0f,
    val metallicFactor: Float = 1.0f,
    val roughnessFactor: Float = 1.0f
) : AssetMetadata()
