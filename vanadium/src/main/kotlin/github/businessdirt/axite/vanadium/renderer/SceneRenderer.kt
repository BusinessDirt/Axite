package github.businessdirt.axite.vanadium.renderer

import github.businessdirt.axite.vanadium.core.profiling.Profiler
import github.businessdirt.axite.vanadium.renderer.scene.Scene
import github.businessdirt.axite.vanadium.vulkan.Context

class SceneRenderer(val context: Context) {

    fun initialize() = Profiler.profile("SceneRenderer Initialization") {

    }

    fun shutdown() = Profiler.profile("SceneRenderer Shutdown"){

    }

    fun drawScene(scene: Scene) {

    }
}