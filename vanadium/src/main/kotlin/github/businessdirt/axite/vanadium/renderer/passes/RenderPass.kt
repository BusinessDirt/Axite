package github.businessdirt.axite.vanadium.renderer.passes

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.core.imgui.ImGuiDrawable
import github.businessdirt.axite.vanadium.vulkan.Context

abstract class RenderPass : ImGuiDrawable {

    val context: Context get() = Vanadium.context

    var isInitialized: Boolean = false
        private set

    suspend fun initialize() {
        if (isInitialized) return

        onInitialize()
        isInitialized = true

        Vanadium.renderer.passes.add(this)
    }

    fun shutdown() {
        if (!isInitialized) return

        onShutdown()
        isInitialized = false
    }

    override fun draw() {
        if (!isInitialized) return
        onImGuiRender()
    }

    protected abstract suspend fun onInitialize()

    protected abstract fun onShutdown()

    protected abstract fun onImGuiRender()

}
