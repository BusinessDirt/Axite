package github.businessdirt.axite.vanadium.core.math

import github.businessdirt.axite.vanadium.FrameInfo

class Clock(updatesPerSecond: Int) {
    private val nsPerUpdate: Double = 1_000_000_000.0 / updatesPerSecond
    private var startTime: Long = System.nanoTime()
    private var previousTime: Long = System.nanoTime()
    private var accumulator: Double = 0.0

    var frameCount: Long = 0
        private set

    /**
     * Updates the internal clock. Should be called once at the start of the frame.
     */
    fun tick() {
        val currentTime = System.nanoTime()
        val elapsedTime = currentTime - previousTime
        previousTime = currentTime
        accumulator += elapsedTime
    }

    /**
     * Checks if enough time has passed to perform a logic update.
     * Consumes one "tick" of the accumulator if true.
     */
    fun shouldUpdate(): Boolean = when {
        accumulator >= nsPerUpdate -> {
            accumulator -= nsPerUpdate
            frameCount++
            true
        }
        else -> false
    }

    /**
     * Calculates the interpolation factor (0.0 to 1.0) for rendering
     * between two fixed update steps.
     */
    val interpolation: Double get() = accumulator / nsPerUpdate

    /**
     * Generates a snapshot of the current time state.
     */
    val frameInfo: FrameInfo get() = FrameInfo(
        deltaTime = nsPerUpdate / 1_000_000_000.0,
        totalTime = (System.nanoTime() - startTime) / 1_000_000_000.0,
        frameCount = frameCount
    )
}