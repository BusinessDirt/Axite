package github.businessdirt.axite.vanadium.utils

import org.slf4j.Logger
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit

sealed class ProfilerOptions(
    open val logger: Logger? = null,
    open val timeUnit: DurationUnit = DurationUnit.MILLISECONDS,
    open val name: String = "Profiler",
    open val message: (duration: Double, unit: DurationUnit) -> String = { duration, unit ->
        "Execution took $duration ${unit.name.lowercase()}"
    }
) {
    // Forces logger and startMessage to null to skip everything
    data object Disabled : ProfilerOptions(logger = null)

    // Requires a logger, but uses all the base defaults
    data class Standard(override val logger: Logger) : ProfilerOptions()

    data class Simple(
        override val logger: Logger,
        override val name: String
    ) : ProfilerOptions()
}

object Profiler {

    inline fun <T> profile(options: ProfilerOptions = ProfilerOptions.Disabled, block: () -> T): T {
        if (options is ProfilerOptions.Disabled) return block()

        val start = System.nanoTime()
        val result = block()
        val elapsedNanos = System.nanoTime() - start

        val durationInUnit = elapsedNanos.nanoseconds.toDouble(options.timeUnit)
        options.logger?.atInfo()?.log(title = options.name) {
            append(options.message.invoke(durationInUnit, options.timeUnit))
        }

        return result
    }
}

fun <T> KFunction<T>.profile(logger: Logger): T =
    Profiler.profile(ProfilerOptions.Simple(logger, this.javaMethod?.declaringClass?.simpleName + "::" + this.name)) {
        this.call()
    }