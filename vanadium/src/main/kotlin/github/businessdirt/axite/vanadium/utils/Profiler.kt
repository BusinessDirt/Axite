package github.businessdirt.axite.vanadium.utils

import org.slf4j.Logger
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit

sealed class ProfilerOptions(
    open val logger: Logger? = null,
    open val timeUnit: DurationUnit = DurationUnit.MILLISECONDS,
    open val startMessage: String = "Starting Profiler",
    open val endMessage: (duration: Double, unit: DurationUnit) -> String = { duration, unit ->
        "Profiling took $duration ${unit.name.lowercase()}"
    }
) {
    // Forces logger and startMessage to null to skip everything
    data object Disabled : ProfilerOptions(
        logger = null,
        startMessage = ""
    )

    // Requires a logger, but uses all the base defaults
    data class Standard(
        override val logger: Logger
    ) : ProfilerOptions(logger = logger)

    data class Simple(
        override val logger: Logger,
        val name: String
    ) : ProfilerOptions(logger = logger, startMessage = "Profiling $name", endMessage = { duration, unit ->
        "$name took $duration ${unit.name.lowercase()}"
    })

    // Allows the caller to override absolutely everything
    data class Complex(
        override val logger: Logger?,
        override val timeUnit: DurationUnit = DurationUnit.MILLISECONDS,
        override val startMessage: String = "Starting Profiler",
        override val endMessage: (Double, DurationUnit) -> String = { duration, unit ->
            "Profiling took $duration ${unit.name.lowercase()}"
        }
    ) : ProfilerOptions(logger, timeUnit, startMessage, endMessage)
}

object Profiler {

    inline fun <T> profile(options: ProfilerOptions = ProfilerOptions.Disabled, block: () -> T): T {
        if (options is ProfilerOptions.Disabled) return block()

        options.logger?.info(options.startMessage)

        val start = System.nanoTime()
        val result = block()
        val elapsedNanos = System.nanoTime() - start

        val durationInUnit = elapsedNanos.nanoseconds.toDouble(options.timeUnit)
        options.logger?.info(options.endMessage(durationInUnit, options.timeUnit))

        return result
    }
}

fun <T> KFunction<T>.profile(logger: Logger): T =
    Profiler.profile(ProfilerOptions.Simple(logger, this.javaMethod?.declaringClass?.simpleName + "::" + this.name)) {
        this.call()
    }