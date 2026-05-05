package github.businessdirt.axite.vanadium.events

abstract class KeyEvent(val keyCode: Int) : Event() {
    override val categoryFlags: Int = (1 shl EventCategory.Input.ordinal) or (1 shl EventCategory.Keyboard.ordinal)
}

class KeyPressedEvent(keyCode: Int, val repeatCount: Int) : KeyEvent(keyCode), Cancellable {
    override val name = "KeyPressedEvent"
    override var isCancelled: Boolean = false
}

class KeyReleasedEvent(keyCode: Int) : KeyEvent(keyCode) {
    override val name = "KeyReleasedEvent"
    override val categoryFlags: Int = super.categoryFlags // Not usually cancellable
}

class KeyTypedEvent(val char: Char) : Event() {
    override val name = "KeyTypedEvent"
    override val categoryFlags: Int = (1 shl EventCategory.Input.ordinal) or (1 shl EventCategory.Keyboard.ordinal)
}