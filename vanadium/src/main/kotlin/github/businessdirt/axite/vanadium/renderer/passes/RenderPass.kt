package github.businessdirt.axite.vanadium.renderer.passes

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.vulkan.Context

abstract class RenderPass {

    val context: Context get() = Vanadium.context

    var isInitialized: Boolean = false
        private set

    suspend fun initialize() {
        if (isInitialized) return

        onInitialize()
        isInitialized = true
    }

    fun shutdown() {
        if (!isInitialized) return

        onShutdown()
        isInitialized = false
    }

    fun renderImGui() {
        if (!isInitialized) return
        onImGuiRender()
    }

    protected abstract suspend fun onInitialize()

    protected abstract fun onShutdown()

    protected abstract fun onImGuiRender()

}
