package github.businessdirt.axite.vanadium.utils

object Profiler {
    inline fun <T> profile(block: () -> T): Pair<Double, T> {
        val start = System.nanoTime()
        val result = block()
        val duration = (System.nanoTime() - start) / 1_000_000.0

        return duration to result
    }
}