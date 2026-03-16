package github.businessdirt.axite.vanadium.platform.vulkan

import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class VulkanHandle<T> {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    abstract val handle: T

    abstract fun destroy()
}