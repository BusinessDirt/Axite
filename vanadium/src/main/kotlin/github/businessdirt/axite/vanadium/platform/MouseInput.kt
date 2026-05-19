package github.businessdirt.axite.vanadium.platform

import github.businessdirt.axite.vanadium.core.events.*
import org.lwjgl.glfw.GLFW.*

object MouseInput {
    var x = 0.0; private set
    var y = 0.0; private set
    var dx = 0.0; private set
    var dy = 0.0; private set
    private val buttons = BooleanArray(GLFW_MOUSE_BUTTON_LAST + 1)
    var scrollX = 0.0; private set
    var scrollY = 0.0; private set

    private var firstMouse = true

    fun onEvent(event: Event) {
        val dispatcher = EventDispatcher(event)
        dispatcher.dispatch<MouseMovedEvent> {
            if (firstMouse) {
                x = it.x
                y = it.y
                firstMouse = false
            }
            dx += it.x - x
            dy += it.y - y
            x = it.x
            y = it.y
        }
        dispatcher.dispatch<MouseButtonPressedEvent> { if (it.button in buttons.indices) buttons[it.button] = true }
        dispatcher.dispatch<MouseButtonReleasedEvent> { if (it.button in buttons.indices) buttons[it.button] = false }
        dispatcher.dispatch<MouseScrolledEvent> {
            scrollX += it.xOffset
            scrollY += it.yOffset
        }
    }

    fun isButtonPressed(button: Int): Boolean = if (button in buttons.indices) buttons[button] else false

    fun endFrame() {
        dx = 0.0
        dy = 0.0
        scrollX = 0.0
        scrollY = 0.0
    }
}