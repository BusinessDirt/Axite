package github.businessdirt.axite.vanadium.platform.vulkan

import github.businessdirt.axite.vanadium.VanadiumConfig

object Context {

    lateinit var instance: Instance
        private set

    var debugMessenger: DebugMessenger? = null
        private set

    fun initialize(config: VanadiumConfig) {
        check(!this::instance.isInitialized) { "Vulkan Context is already initialized!" }

        instance = Instance(config)
        if (config.validate) debugMessenger = DebugMessenger(instance.handle)
    }

    fun shutdown() {
        if (!this::instance.isInitialized) return

        debugMessenger?.destroy()
        instance.destroy()

        debugMessenger = null
    }
}