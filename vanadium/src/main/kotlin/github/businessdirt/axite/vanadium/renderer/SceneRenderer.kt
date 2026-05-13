package github.businessdirt.axite.vanadium.renderer

import github.businessdirt.axite.vanadium.core.profiling.Profiler
import github.businessdirt.axite.vanadium.vulkan.Context
import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer

class SceneRenderer(val context: Context) {

    fun initialize() = Profiler.profile("SceneRenderer Initialization") {

    }

    fun shutdown() = Profiler.profile("SceneRenderer Shutdown"){

    }
}