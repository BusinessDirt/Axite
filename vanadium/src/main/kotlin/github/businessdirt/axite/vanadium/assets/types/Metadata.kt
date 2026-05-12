package github.businessdirt.axite.vanadium.assets.types

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
sealed class AssetMetadata {
    abstract val uuid: String
    abstract val version: Int
}

@Serializable
data class ShaderMetadata(
    override val uuid: String = UUID.randomUUID().toString(),
    override val version: Int = 1,
    val hash: String = "",
    val stage: ShaderStage,
    val compilationTime: Long = 0L,
    val definitions: Map<String, String> = emptyMap()
) : AssetMetadata()
