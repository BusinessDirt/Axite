package github.businessdirt.axite.vanadium.renderer.passes

import github.businessdirt.axite.vanadium.vulkan.Context

abstract class RenderPass(val context: Context) {

    abstract suspend fun initialize()

    abstract fun shutdown()

}
