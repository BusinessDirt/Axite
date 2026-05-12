package github.businessdirt.axite.vanadium.assets

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

interface Asset : AutoCloseable {

    val path: String

    val logger: Logger
        get() = LogManager.getLogger(this::class.java)

    fun release()

    override fun close() {
        logger.debug("Releasing {} [{}]", this::class.simpleName, this.path)
        release()
    }
}

interface AssetLoader<T : Asset> {
    suspend fun load(path: String): T
}