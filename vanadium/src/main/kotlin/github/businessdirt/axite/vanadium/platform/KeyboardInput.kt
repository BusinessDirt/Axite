package github.businessdirt.axite.vanadium.platform

import github.businessdirt.axite.vanadium.core.events.Event
import github.businessdirt.axite.vanadium.core.events.EventDispatcher
import github.businessdirt.axite.vanadium.core.events.KeyPressedEvent
import github.businessdirt.axite.vanadium.core.events.KeyReleasedEvent
import org.lwjgl.glfw.GLFW.*

object KeyboardInput {
    private val keys = BooleanArray(GLFW_KEY_LAST + 1)

    fun onEvent(event: Event) {
        val dispatcher = EventDispatcher(event)
        dispatcher.dispatch<KeyPressedEvent> { keys[it.keyCode] = true }
        dispatcher.dispatch<KeyReleasedEvent> { keys[it.keyCode] = false }
    }

    fun isKeyPressed(keyCode: Int): Boolean = if (keyCode in keys.indices) keys[keyCode] else false
}