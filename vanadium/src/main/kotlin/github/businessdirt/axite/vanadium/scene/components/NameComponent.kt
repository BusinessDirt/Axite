package github.businessdirt.axite.vanadium.scene.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import github.businessdirt.axite.vanadium.core.imgui.ImGuiDrawable
import imgui.ImGui
import imgui.type.ImString

data class NameComponent(
    var name: String = "Entity"
) : Component<NameComponent>, ImGuiDrawable {
    companion object : ComponentType<NameComponent>()

    override fun type() = NameComponent

    override fun draw() {
        val nameString = ImString(name, 100)
        if (ImGui.inputText("Name", nameString)) {
            name = nameString.get()
        }
    }
}
