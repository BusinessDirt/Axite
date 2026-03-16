package github.businessdirt.axite.vanadium.graph

import github.businessdirt.axite.vanadium.VanadiumConfig
import github.businessdirt.axite.vanadium.platform.vulkan.Context

object RenderGraph {

    fun initialize(config: VanadiumConfig) {
        Context.initialize(config)
    }

    fun shutdown() {
        Context.shutdown()
    }

    fun render() {
        // To be implemented
    }
}