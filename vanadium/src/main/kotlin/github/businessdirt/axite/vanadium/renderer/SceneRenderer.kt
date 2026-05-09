package github.businessdirt.axite.vanadium.renderer

import github.businessdirt.axite.vanadium.core.profiling.Profiler
import github.businessdirt.axite.vanadium.renderer.scene.Scene
import github.businessdirt.axite.vanadium.vulkan.Context
import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer

class SceneRenderer(val context: Context) {

    fun initialize() = Profiler.profile("SceneRenderer Initialization") {

    }

    fun shutdown() = Profiler.profile("SceneRenderer Shutdown"){

    }

    fun drawScene(scene: Scene, commandBuffer: CommandBuffer, interpolation: Double) {
        // Clearing is handled by the RenderGraph's automated beginRendering call
    }
}