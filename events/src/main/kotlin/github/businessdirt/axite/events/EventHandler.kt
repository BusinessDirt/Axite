package github.businessdirt.axite.events

import java.util.function.Consumer
import java.util.function.ToIntFunction
import kotlin.reflect.KClass

/**
 * Handles the posting of a specific event type to its listeners.
 * It manages a sorted list of listeners and invokes them in order of priority.
 */
class EventHandler(
    event: KClass<out Event>,
    listeners: MutableList<EventListener>
) {
    /**
     * @return the simple name of the event this handler is for.
     */
    val name: String
    private val listeners: MutableList<EventListener>
    private val canReceiveCancelled: Boolean

    /**
     * Constructs a new [EventHandler].
     *
     * @param event     the event class this handler is for.
     * @param listeners the list of listeners for this event.
     */
    init {
        val eventName = event.qualifiedName ?: "Event"
        val parts = eventName.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val lastPart = if (parts.isNotEmpty()) parts[parts.size - 1] else eventName
        this.name = lastPart.replace("$", ".")

        this.listeners = ArrayList(listeners)
        this.listeners.sortWith(Comparator.comparingInt<EventListener>(
            ToIntFunction { listener: EventListener -> listener.priority }))
        this.canReceiveCancelled = this.listeners.stream().anyMatch(EventListener::canReceiveCancelled)
    }

    /**
     * Posts an event to all its listeners.
     *
     * @param event   the event to post.
     * @param onError a [Consumer] that will be called if an exception is thrown by a listener.
     * @return `true` if the event was cancelled by any of the listeners, `false` otherwise.
     */
    fun post(
        event: Event,
        onError: Consumer<Throwable>?
    ): Boolean {
        if (this.listeners.isEmpty()) return false
        for (listener in this.listeners) {
            if (!listener.shouldInvoke(event)) continue

            try {
                listener.invoker.accept(event)
            } catch (throwable: Throwable) {
                onError?.accept(throwable)
            }

            if (event.isCancelled && !this.canReceiveCancelled) break
        }

        return event.isCancelled
    }
}