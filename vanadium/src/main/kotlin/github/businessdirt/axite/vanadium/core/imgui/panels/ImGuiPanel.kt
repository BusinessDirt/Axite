package github.businessdirt.axite.vanadium.core.imgui.panels

import github.businessdirt.axite.vanadium.core.imgui.ImGuiDrawable
import github.businessdirt.axite.vanadium.core.imgui.ImGuiUtils.window

abstract class ImGuiPanel(
    val displayName: String,
) : ImGuiDrawable {

    var enabled = true

    init {
        registeredPanels.add(this)
    }

    override fun draw() = window(displayName, block = ::drawContent)

    protected abstract fun drawContent()

    override fun hashCode(): Int = displayName.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImGuiPanel

        if (enabled != other.enabled) return false
        if (displayName != other.displayName) return false

        return true
    }

    companion object {
        val registeredPanels = mutableSetOf<ImGuiPanel>()

        fun drawAllEnabled() = registeredPanels.filter { it.enabled }.forEach { it.draw() }
    }
}