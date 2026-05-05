package github.businessdirt.axite.vanadium.vulkan.device

import github.businessdirt.axite.vanadium.VanadiumConfig
import github.businessdirt.axite.vanadium.core.profiling.Profiler
import github.businessdirt.axite.vanadium.platform.Window
import org.slf4j.LoggerFactory

class VulkanContext(private val config: VanadiumConfig) {
    private val logger = LoggerFactory.getLogger(VulkanContext::class.java)

    fun initialize(window: Window) = Profiler.profile("Vulkan Context Initialization") {

    }

    fun shutdown() = Profiler.profile("Vulkan Context Initialization") {

    }
}