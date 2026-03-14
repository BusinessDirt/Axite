package github.businessdirt.axite.vanadium.events.glfw

import github.businessdirt.axite.events.Event

data class MouseMovedEvent(val x: Double, val y: Double) : Event()
data class MousePressedEvent(val button: Int, val mods: Int) : Event()
data class MouseReleasedEvent(val button: Int, val mods: Int) : Event()