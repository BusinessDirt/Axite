package github.businessdirt.axite.events

/**
 * Use @[HandleEvent]
 */
abstract class Event protected constructor() {

    // TODO: This should only be accessible in the cancellable interface
    var isCancelled: Boolean = false
        private set

    fun post() = prePost(onError = null)

    fun post(onError: (Throwable) -> Unit = {}) = prePost(onError)

    private fun prePost(onError: ((Throwable) -> Unit)?): Boolean =
        EventBus.getEventHandler(this::class).post(this, onError)

    interface Cancelable {
        fun cancel() {
            val event = this as Event
            event.isCancelled = true
        }
    }
}