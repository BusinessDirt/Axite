package github.businessdirt.axite.vanadium.assets.metadata

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class ModelMetadata(
    override val uuid: String = UUID.randomUUID().toString(),
    override val version: Int = 1,
    val meshCount: Int = 0,
    val materialCount: Int = 0
) : AssetMetadata()
