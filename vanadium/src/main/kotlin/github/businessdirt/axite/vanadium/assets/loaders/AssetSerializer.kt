package github.businessdirt.axite.vanadium.assets.loaders

import github.businessdirt.axite.vanadium.assets.metadata.AssetMetadata
import github.businessdirt.axite.vanadium.assets.types.Asset
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File

abstract class AssetSerializer<T : Asset, M : AssetMetadata>(
    private val metadataSerializer: KSerializer<M>
) {

    protected val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    val logger: Logger = LogManager.getLogger(this::class.java)

    abstract suspend fun load(path: String): T

    fun loadMetadata(path: String): M? {
        if (!hasMetadata(path)) return null

        return try {
            json.decodeFromString(metadataSerializer, File("$path.meta").readText())
        } catch (e: Exception) {
            logger.error("Failed to load metadata for [{}]: {}", path, e.message)
            null
        }
    }

    fun writeMetadata(path: String, metadata: M) = try {
        val text = json.encodeToString(metadataSerializer, metadata)
        File("$path.meta").writeText(text)
    } catch (e: Exception) {
        logger.error("Failed to write metadata for [{}]: {}", path, e.message)
    }

    fun hasMetadata(path: String): Boolean = File("$path.meta").exists()
}