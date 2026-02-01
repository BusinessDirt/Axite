package github.businessdirt.axite.events

/**
 * Use @[HandleEvent]
 */
abstract class Event protected constructor() {

    fun post(onError: (Throwable) -> Unit = {}) =
        EventBus.getEventHandler(this::class).post(this, onError)

    interface Cancelable {
        var isCancelled: Boolean

        fun cancel() {
            this.isCancelled = true
        }
    }
}