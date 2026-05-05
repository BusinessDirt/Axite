package github.businessdirt.axite.vanadium.events

enum class EventCategory {
    None, Application, Input, Keyboard, Mouse, MouseButton
}

abstract class Event {
    var isHandled: Boolean = false

    abstract val name: String
    abstract val categoryFlags: Int // Use bitmasking for multiple categories

    fun isInCategory(category: EventCategory): Boolean {
        return (categoryFlags and (1 shl category.ordinal)) != 0
    }

    override fun toString(): String = name
}

interface Cancellable {
    var isCancelled: Boolean
}