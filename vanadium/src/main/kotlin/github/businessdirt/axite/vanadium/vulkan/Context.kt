package github.businessdirt.axite.vanadium.vulkan

import github.businessdirt.axite.vanadium.VanadiumConfig
import github.businessdirt.axite.vanadium.core.profiling.Profiler
import github.businessdirt.axite.vanadium.platform.Window
import github.businessdirt.axite.vanadium.vulkan.device.PhysicalDevice
import github.businessdirt.axite.vanadium.vulkan.device.pickPhysicalDevice
import org.apache.logging.log4j.LogManager

class Context(private val config: VanadiumConfig) {
    private val logger = LogManager.getLogger(Context::class.java)

    private val scope = ResourceScope()

    lateinit var instance: Instance
    lateinit var debugMessenger: DebugMessenger
    lateinit var physicalDevice: PhysicalDevice

    fun initialize(window: Window) = Profiler.profile("Vulkan Context Initialization") {
        instance = scope.use(Instance(config))
        if (config.validate) debugMessenger = scope.use(DebugMessenger(instance.handle))
        physicalDevice = scope.use(instance.pickPhysicalDevice())
    }

    fun shutdown() = Profiler.profile("Vulkan Context Initialization") {
        scope.close()
    }
}

