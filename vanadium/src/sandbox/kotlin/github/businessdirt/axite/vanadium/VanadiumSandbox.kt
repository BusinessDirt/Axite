package github.businessdirt.axite.vanadium

import github.businessdirt.axite.vanadium.core.events.Event
import github.businessdirt.axite.vanadium.renderer.SceneRenderer
import github.businessdirt.axite.vanadium.renderer.graph.RenderGraph
import github.businessdirt.axite.vanadium.renderer.scene.Scene
import kotlinx.coroutines.CoroutineScope


class VanadiumSandbox : VanadiumAdapter {

    private val scene: Scene = Scene()

    override suspend fun initialize(scope: CoroutineScope) {

    }

    override fun update(frameInfo: FrameInfo) {

    }

    override fun onRecord(graph: RenderGraph, sceneRenderer: SceneRenderer, interpolation: Double) {

        graph.addPass(
            name = "MainScenePass",
            writes = setOf("SwapChainImage", "DepthBuffer")
        ) {
            sceneRenderer.drawScene(scene)
        }
    }

    override fun onEvent(event: Event) {

    }
}