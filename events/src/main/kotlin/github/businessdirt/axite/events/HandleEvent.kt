package github.businessdirt.axite.events

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class HandleEvent(
    /**
     * For cases where the event properties are themselves not needed, and solely a listener for an event fire suffices.
     */
    val eventType: KClass<out Event> = Event::class,

    /**
     * The priority of when the event will be called, lower priority will be called first, see the companion object.
     */
    val priority: Int = MEDIUM,

    /**
     * If the event is cancelled & receiveCancelled is true, then the method will still invoke.
     */
    val receiveCancelled: Boolean = false,
) {

    companion object {
        const val HIGHEST = -2
        const val HIGH = -1
        const val MEDIUM = 0
        const val LOW = 1
        const val LOWEST = 2
    }
}
