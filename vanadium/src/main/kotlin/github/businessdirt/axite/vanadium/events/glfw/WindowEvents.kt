package github.businessdirt.axite.vanadium.events.glfw

import github.businessdirt.axite.events.Event

data class WindowResizedEvent(val width: Int, val height: Int) : Event()