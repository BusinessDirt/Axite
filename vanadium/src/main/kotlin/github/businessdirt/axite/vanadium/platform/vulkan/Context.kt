package github.businessdirt.axite.vanadium.platform.vulkan

import github.businessdirt.axite.vanadium.VanadiumConfig
import github.businessdirt.axite.vanadium.platform.Window

object Context {

    lateinit var instance: Instance
        private set

    var debugMessenger: DebugMessenger? = null
        private set

    lateinit var physicalDevice: PhysicalDevice
        private set

    lateinit var device: Device
        private set

    lateinit var surface: Surface
        private set

    fun initialize(window: Window, config: VanadiumConfig) {
        check(!this::instance.isInitialized) { "Vulkan Context is already initialized!" }

        instance = Instance(config)
        if (config.validate) debugMessenger = DebugMessenger(instance.handle)
        physicalDevice = instance.pickPhysicalDevice()
        device = Device(physicalDevice)
        surface = Surface(physicalDevice, instance, window.handle)
    }

    fun shutdown() {
        if (!this::instance.isInitialized) return

        surface.destroy()
        device.destroy()
        physicalDevice.destroy()
        debugMessenger?.destroy()
        instance.destroy()

        debugMessenger = null
    }
}