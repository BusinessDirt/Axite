package github.businessdirt.axite.vanadium.assets.types

import github.businessdirt.axite.vanadium.assets.metadata.AssetMetadata
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.concurrent.atomic.AtomicInteger

abstract class Asset(
    open val uuid: String,
    open val path: String,
    open val metadata: AssetMetadata
) : AutoCloseable {

    protected val refCount = AtomicInteger(1)

    val logger: Logger = LogManager.getLogger(this::class.java)

    /**
     * Increments the reference count of the asset.
     */
    fun retain() {
        refCount.incrementAndGet()
    }

    /**
     * Decrements the reference count and disposes of the asset if the count reaches zero.
     * @return True if the asset was disposed, false otherwise.
     */
    fun release(): Boolean {
        if (refCount.decrementAndGet() <= 0) {
            logger.debug("Disposing {} [{}]", this::class.simpleName, this.path)
            dispose()
            return true
        }
        return false
    }

    /**
     * Internal logic to free up resources held by the asset.
     */
    protected abstract fun dispose()

    override fun close() {
        release()
    }
}
