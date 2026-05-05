package github.businessdirt.axite.vanadium.events

abstract class ApplicationEvent : Event() {
    override val categoryFlags: Int = (1 shl EventCategory.Application.ordinal)
}

class WindowResizedEvent(val width: Int, val height: Int) : ApplicationEvent() {
    override val name = "WindowResizedEvent"
}

class WindowClosedEvent : ApplicationEvent(), Cancellable {
    override val name = "WindowClosedEvent"
    override var isCancelled: Boolean = false
}

class WindowFocusEvent(val hasFocus: Boolean) : ApplicationEvent() {
    override val name = "WindowFocusEvent"
}

class WindowMovedEvent(val x: Int, val y: Int) : ApplicationEvent() {
    override val name = "WindowMovedEvent"
}