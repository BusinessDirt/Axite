package github.businessdirt.axite.vanadium.vulkan

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

abstract class Handle<T> {
    val logger: Logger = LogManager.getLogger(this::class.java)

    abstract val handle: T

    fun cleanup() {
        logger.debug("Destroying {} [handle: {}]", this::class.simpleName, handle)
        destroy()
    }

    protected abstract fun destroy()
}