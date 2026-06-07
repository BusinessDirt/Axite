package github.businessdirt.axite.vanadium.core.imgui.panels

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.core.imgui.ImGuiDrawable
import github.businessdirt.axite.vanadium.scene.Entity
import github.businessdirt.axite.vanadium.scene.components.*
import imgui.ImGui
import imgui.flag.ImGuiTreeNodeFlags

object PropertiesPanel : ImGuiPanel("Properties") {

    override fun drawContent() {
        val scene = Vanadium.scene ?: return
        val entity = scene.selectedEntity ?: return

        entity.drawComponentHelper(NameComponent, "Name")
        entity.drawComponentHelper(TransformComponent, "Transform")
        entity.drawComponentHelper(CameraComponent, "Camera")
        entity.drawComponentHelper(CameraControllerComponent, "Camera Controller")
        entity.drawComponentHelper(ModelComponent, "Model")
    }

    private inline fun <reified T : Component<T>> Entity.drawComponentHelper(
        component: ComponentType<T>,
        label: String
    ) {
        if (!has(component)) return
        if (!ImGui.collapsingHeader(label, ImGuiTreeNodeFlags.DefaultOpen)) return

        val comp = this[component]
        if (comp is ImGuiDrawable) comp.draw()
    }
}
