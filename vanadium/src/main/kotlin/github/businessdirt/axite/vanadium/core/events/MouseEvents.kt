package github.businessdirt.axite.vanadium.core.events

abstract class MouseEvent : Event() {
    override val categoryFlags: Int = (1 shl EventCategory.Input.ordinal) or (1 shl EventCategory.Mouse.ordinal)
}

class MouseMovedEvent(val x: Double, val y: Double) : MouseEvent() {
    override val name = "MouseMovedEvent"
}

class MouseScrolledEvent(val xOffset: Double, val yOffset: Double) : MouseEvent(), Cancellable {
    override val name = "MouseScrolledEvent"
    override var isCancelled: Boolean = false
}

abstract class MouseButtonEvent(val button: Int) : MouseEvent() {
    override val categoryFlags: Int = super.categoryFlags or (1 shl EventCategory.MouseButton.ordinal)
}

class MouseButtonPressedEvent(button: Int) : MouseButtonEvent(button), Cancellable {
    override val name = "MouseButtonPressedEvent"
    override var isCancelled: Boolean = false
}

class MouseButtonReleasedEvent(button: Int) : MouseButtonEvent(button) {
    override val name = "MouseButtonReleasedEvent"
}