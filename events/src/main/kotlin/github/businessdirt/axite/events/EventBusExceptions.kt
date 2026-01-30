package github.businessdirt.axite.events

import kotlin.reflect.KFunction

class ClassNotInstantiableException(function: KFunction<*>) : RuntimeException(
    "Event Listener ${function.name} must be inside a kotlin object"
)

class MethodNotPublicException(function: KFunction<*>) : RuntimeException(
    "Method ${ReflectionUtils.getFunctionString(function)} is not public"
)

class ParameterException(
    function: KFunction<*>,
    extra: String,
) : RuntimeException(
    "Method ${ReflectionUtils.getFunctionString(function)} $extra"
)

class InvalidConsumerException(
    function: KFunction<*>,
    cause: Throwable?
) : RuntimeException(
    "Method ${ReflectionUtils.getFunctionString(function)} is not a valid consumer. " +
            getInternalExecutableCause(function, 1),
    cause
)

class InvalidRunnableException(
    function: KFunction<*>,
    cause: Throwable
) : RuntimeException(
    "Method ${ReflectionUtils.getFunctionString(function)} is not a valid runnable: " +
            getInternalExecutableCause(function, 0),
    cause
)

private fun getInternalExecutableCause(
    function: KFunction<*>,
    expectedSize: Int,
): String {
    if (function.parameters.size != expectedSize)
        return "Expected parameter count of $expectedSize but was ${function.parameters.size}"

    return "Unknown reason."
}