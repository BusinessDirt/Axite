package github.businessdirt.axite.events

import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * Creates a new [EventListener] from the given parameters.
 *
 * @param name    the name of the listener method.
 * @param invoker a [Consumer] that invokes the listener method.
 * @param options the [HandleEvent] annotation of the listener method.
 * @return a new [EventListener] owner.
 */
class EventListener(
    val name: String,
    val invoker: Consumer<Event>,
    options: HandleEvent,
    eventClass: KClass<out Event>,
) {

    val priority: Int = options.priority
    val canReceiveCancelled: Boolean = options.receiveCancelled
    val predicates: MutableList<Predicate<Event>> = ArrayList()

    init {
        if (eventClass.isSubclassOf(CancelableEvent::class) &&
            !options.receiveCancelled) predicates.add(Predicate { event: Event ->
                !(event as CancelableEvent).isCancelled
            })
    }

    /**
     * Checks if the listener should be invoked for the given event.
     *
     * @param event the event to check.
     * @return `true` if the listener should be invoked, `false` otherwise.
     */
    fun shouldInvoke(event: Event): Boolean =
        predicates.stream().allMatch { predicate: Predicate<Event> -> predicate.test(event) }
}