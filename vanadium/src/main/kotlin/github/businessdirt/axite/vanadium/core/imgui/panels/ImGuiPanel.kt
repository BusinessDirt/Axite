package github.businessdirt.axite.vanadium.core.imgui.panels

import github.businessdirt.axite.vanadium.core.imgui.ImGuiDrawable
import github.businessdirt.axite.vanadium.core.imgui.ImGuiUtils.window

abstract class ImGuiPanel(
    val displayName: String,
) : ImGuiDrawable {

    init {
        registeredPanels[displayName] = this
    }

    override fun draw() = window(displayName, block = ::drawContent)

    protected abstract fun drawContent()

    companion object {
        val registeredPanels = mutableMapOf<String, ImGuiPanel>()
    }
}