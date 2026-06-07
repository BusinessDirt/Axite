package github.businessdirt.axite.vanadium.core.imgui.panels

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.scene.components.*
import imgui.ImGui
import imgui.flag.ImGuiTreeNodeFlags

object PropertiesPanel : ImGuiPanel("Properties") {

    override fun drawContent() {
        val scene = Vanadium.scene ?: return
        val entity = scene.selectedEntity ?: return

        if (entity.has(NameComponent)) {
            if (ImGui.collapsingHeader("Name", ImGuiTreeNodeFlags.DefaultOpen)) {
                entity[NameComponent].draw()
            }
        }

        if (entity.has(TransformComponent)) {
            if (ImGui.collapsingHeader("Transform", ImGuiTreeNodeFlags.DefaultOpen)) {
                entity[TransformComponent].draw()
            }
        }

        if (entity.has(CameraComponent)) {
            if (ImGui.collapsingHeader("Camera", ImGuiTreeNodeFlags.DefaultOpen)) {
                entity[CameraComponent].draw()
            }
        }

        if (entity.has(CameraControllerComponent)) {
            if (ImGui.collapsingHeader("Camera Controller", ImGuiTreeNodeFlags.DefaultOpen)) {
                entity[CameraControllerComponent].draw()
            }
        }

        if (entity.has(ModelComponent)) {
            if (ImGui.collapsingHeader("Model", ImGuiTreeNodeFlags.DefaultOpen)) {
                entity[ModelComponent].draw()
            }
        }
    }
}
