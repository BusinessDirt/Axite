package github.businessdirt.axite.vanadium.scene.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import github.businessdirt.axite.vanadium.assets.types.Model
import github.businessdirt.axite.vanadium.core.imgui.ImGuiDrawable
import imgui.ImGui

data class ModelComponent(
    var model: Model? = null
) : Component<ModelComponent>, ImGuiDrawable {
    companion object : ComponentType<ModelComponent>()

    override fun type() = ModelComponent

    override fun draw() {
        ImGui.text("Model: ${model?.path ?: "None"}")
    }
}
