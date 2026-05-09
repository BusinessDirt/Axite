package github.businessdirt.axite.vanadium

import github.businessdirt.axite.vanadium.core.events.Event
import github.businessdirt.axite.vanadium.core.math.Resolution
import github.businessdirt.axite.vanadium.renderer.SceneRenderer
import github.businessdirt.axite.vanadium.renderer.graph.RenderGraph
import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer
import kotlinx.coroutines.CoroutineScope

data class VanadiumConfig(
    var applicationName: String = "Vanadium Application",
    var resolution: Resolution = Resolution(1920, 1080),
    var updatesPerSecond: Int = 60,
    var validate: Boolean = true,
    var ioParallelism: Int = 4,
    var requestedImages: Int = 3,
    var vsync: Boolean = true,
)

data class FrameInfo(
    val deltaTime: Double,
    val totalTime: Double,
    val frameCount: Long
)

interface VanadiumAdapter {
    fun configure(config: VanadiumConfig) {}
    suspend fun initialize(scope: CoroutineScope)
    fun update(frameInfo: FrameInfo)

    /**
     * Instead of raw rendering, the adapter populates the RenderGraph.
     * @param graph The graph to add RenderPassNodes to.
     * @param sceneRenderer The scene renderer that can render a scene.
     * @param commandBuffer The command buffer of the current frame.
     * @param interpolation The alpha value for jitter-free rendering.
     */
    fun onRecord(graph: RenderGraph, sceneRenderer: SceneRenderer, commandBuffer: CommandBuffer, interpolation: Double)

    fun onEvent(event: Event)
    fun shutdown() {}
}