package github.businessdirt.axite.vanadium

import github.businessdirt.axite.vanadium.core.events.Event
import github.businessdirt.axite.vanadium.core.math.Resolution
import kotlinx.coroutines.CoroutineScope

data class VanadiumConfig(
    var applicationName: String = "Vanadium Application",
    var resolution: Resolution = Resolution(1920, 1080),
    var updatesPerSecond: Int = 60,
    var validate: Boolean = true,
    var ioParallelism: Int = 4,
    var requestedImages: Int = 2,
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
    fun render(interpolation: Double)
    fun onEvent(event: Event)
    fun shutdown() {}
}