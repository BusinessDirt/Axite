package github.businessdirt.axite.vanadium.assets.loaders

import github.businessdirt.axite.vanadium.assets.types.Asset
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

interface AssetSerializer<T : Asset> {

    val logger: Logger
        get() = LogManager.getLogger(this::class.java)

    suspend fun load(path: String): T
}