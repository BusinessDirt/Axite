package github.businessdirt.axite.vanadium.vulkan.device

import github.businessdirt.axite.vanadium.VanadiumConfig
import github.businessdirt.axite.vanadium.platform.Window
import org.slf4j.LoggerFactory

class VulkanContext(private val config: VanadiumConfig) {
    private val logger = LoggerFactory.getLogger(VulkanContext::class.java)

    fun initialize(window: Window) {
        logger.info("Initializing Vulkan Context...")
        logger.info("Vulkan Context initialized successfully.")
    }

    fun shutdown() {
        logger.info("Destroying Vulkan Context...")
    }
}