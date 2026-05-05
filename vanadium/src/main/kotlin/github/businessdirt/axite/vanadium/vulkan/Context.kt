package github.businessdirt.axite.vanadium.vulkan

import github.businessdirt.axite.vanadium.VanadiumConfig
import github.businessdirt.axite.vanadium.core.profiling.Profiler
import github.businessdirt.axite.vanadium.platform.Window
import org.slf4j.LoggerFactory

class Context(private val config: VanadiumConfig) {
    private val logger = LoggerFactory.getLogger(Context::class.java)

    lateinit var instance: Instance

    fun initialize(window: Window) = Profiler.profile("Vulkan Context Initialization") {
        instance = Instance(config)
    }

    fun shutdown() = Profiler.profile("Vulkan Context Initialization") {
        instance.cleanup()
    }
}

