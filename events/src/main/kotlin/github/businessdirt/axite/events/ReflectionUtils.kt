package github.businessdirt.axite.events

import java.lang.invoke.LambdaMetafactory
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.function.Consumer
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

object ReflectionUtils {

    fun getFunctionString(function: KFunction<*>): String =
        "${function.name}(${function.parameters.joinToString(", ")})"

    /**
     * Creates a [Consumer] that invokes the given method on the provided owner.
     * The method must be public, non-static, and take exactly one argument.
     *
     * @param instance the object on which the method will be invoked.
     * @param function the function to be invoked.
     * @return a [Consumer] that, when called, invokes the specified method.
     * @throws InvalidConsumerException if the method is not a valid consumer (e.g., wrong number of arguments).
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(InvalidConsumerException::class)
    fun createSingleParameterEventConsumer(
        instance: Any,
        function: KFunction<*>
    ): Consumer<Event> {
        try {
            val lookup = MethodHandles.lookup()
            val handle = lookup.unreflect(function.javaMethod)

            // The specific type the method expects (e.g., ApplicationStartEvent)
            val specificEventType = function.parameters[0].type.javaClass

            // Create a Consumer via LambdaMetafactory
            val site = LambdaMetafactory.metafactory(
                lookup,
                "accept",
                MethodType.methodType(Consumer::class.java, instance.javaClass),  // Factory signature
                MethodType.methodType(Void.TYPE, Any::class.java),  // Interface type
                handle,
                MethodType.methodType(Void.TYPE, specificEventType) // Enforced type
            )

            // Return the auto-casting Consumer
            return site.getTarget().bindTo(instance).invokeExact() as Consumer<Event>
        } catch (e: Throwable) {
            throw InvalidConsumerException(function, e)
        }
    }

    /**
     * Creates a [Consumer] that invokes the given method on the provided owner.
     * The method must be public, non-static, and take no arguments.
     *
     * @param instance the object on which the method will be invoked.
     * @param function the function to be invoked.
     * @return a [Consumer] that, when called, invokes the specified method.
     * @throws InvalidRunnableException if the method is not a valid runnable (e.g., wrong number of arguments).
     */
    @Throws(InvalidRunnableException::class)
    fun createZeroParameterEventConsumer(
        instance: Any,
        function: KFunction<*>
    ): Consumer<Event> {
        try {
            val lookup = MethodHandles.lookup()
            val handle = lookup.unreflect(function.javaMethod)

            // Create a high-performance Runnable via LambdaMetafactory
            val site = LambdaMetafactory.metafactory(
                lookup,
                "run",
                MethodType.methodType(Runnable::class.java, instance.javaClass),  // Factory signature
                MethodType.methodType(Void.TYPE),  // Interface type
                handle,
                MethodType.methodType(Void.TYPE) // Enforced type
            )

            val fastRunnable = site.getTarget().bindTo(instance).invokeExact() as Runnable
            return Consumer { `_`: Event -> fastRunnable.run() }
        } catch (e: Throwable) {
            throw InvalidRunnableException(function, e)
        }
    }
}