package github.businessdirt.axite.vanadium.assets.metadata

import kotlinx.serialization.Serializable

@Serializable
sealed class AssetMetadata {
    abstract val uuid: String
    abstract val version: Int
}