package github.businessdirt.axite.vanadium.events.glfw

import github.businessdirt.axite.events.Event

data class KeyPressedEvent(val key: Int, val mods: Int) : Event()
data class KeyReleasedEvent(val key: Int, val mods: Int) : Event()