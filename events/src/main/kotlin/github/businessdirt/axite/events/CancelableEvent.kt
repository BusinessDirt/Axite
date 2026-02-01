package github.businessdirt.axite.events

abstract class CancelableEvent : Event(), Event.Cancelable {
    override var isCancelled: Boolean = false
}