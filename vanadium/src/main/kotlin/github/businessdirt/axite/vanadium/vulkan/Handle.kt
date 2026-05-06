package github.businessdirt.axite.vanadium.vulkan

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

abstract class Handle<T> : AutoCloseable {
    val logger: Logger = LogManager.getLogger(this::class.java)

    abstract val handle: T

    override fun close() {
        logger.debug("Destroying {} [handle: {}]", this::class.simpleName, handle)
        destroy()
    }

    protected abstract fun destroy()
}

class ResourceScope : AutoCloseable {
    private val resources = mutableListOf<AutoCloseable>()

    fun <T : AutoCloseable> use(resource: T): T {
        resources.add(resource)
        return resource
    }

    override fun close() = resources.reversed().forEach { it.close() }
}