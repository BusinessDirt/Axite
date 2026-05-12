package github.businessdirt.axite.vanadium.assets.loaders

import github.businessdirt.axite.vanadium.assets.types.Asset
import github.businessdirt.axite.vanadium.assets.types.AssetMetadata
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File

interface AssetSerializer<T : Asset, M : AssetMetadata> {

    val logger: Logger
        get() = LogManager.getLogger(this::class.java)

    /**
     * The main entry point. Orchestrates loading metadata and then the asset.
     */
    suspend fun load(path: String): T

    /**
     * Loads just the metadata.
     * Returns M?, as the meta file might not exist yet.
     */
    fun loadMetadata(path: String): M?

    /**
     * Persists metadata to disk.
     */
    fun writeMetadata(path: String, metadata: M)

    /**
     * Utility to check for the sidecar file.
     */
    fun hasMetadata(path: String): Boolean = File("$path.meta").exists()
}