package github.businessdirt.axite.vanadium.events

class EventDispatcher(val event: Event) {

    /**
     * Dispatches the event to a handler if the type matches.
     * @return True if the event was dispatched and handled.
     */
    inline fun <reified T : Event> dispatch(handler: (T) -> Unit): Boolean {
        // Check if the event matches the requested type T
        // Ensure the event hasn't been handled by a previous layer/listener
        if (event is T && !event.isHandled) {

            // If it's a Cancellable event and already cancelled,
            // we usually stop dispatching to lower-priority layers.
            if (event is Cancellable && event.isCancelled) return false

            handler(event)
            return true
        }

        return false
    }

    /**
     * Optional: Dispatch based on category rather than specific type.
     * Useful for global systems (e.g., an Input Manager that logs ALL keyboard events).
     */
    fun dispatchByCategory(category: EventCategory, handler: (Event) -> Unit) {
        if (event.isInCategory(category) && !event.isHandled) handler(event)
    }
}