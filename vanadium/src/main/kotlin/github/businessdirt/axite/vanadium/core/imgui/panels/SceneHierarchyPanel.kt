package github.businessdirt.axite.vanadium.core.imgui.panels

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.scene.Entity
import github.businessdirt.axite.vanadium.scene.components.HierarchyComponent
import github.businessdirt.axite.vanadium.scene.components.NameComponent
import imgui.ImGui
import imgui.flag.ImGuiTreeNodeFlags

object SceneHierarchyPanel : ImGuiPanel("Scene Hierarchy") {

    override fun drawContent() {
        val scene = Vanadium.scene ?: return

        scene.forEachRootEntity { entity ->
            drawEntityNode(entity)
        }

        if (ImGui.isMouseDown(0) && ImGui.isWindowHovered()) {
            scene.selectedEntity = null
        }
    }

    private fun drawEntityNode(entity: Entity) {
        val scene = Vanadium.scene ?: return
        val name = entity[NameComponent].name
        val hierarchy = entity[HierarchyComponent]

        val flags = (if (scene.selectedEntity == entity) ImGuiTreeNodeFlags.Selected else 0) or
                (if (hierarchy.children.isEmpty()) ImGuiTreeNodeFlags.Leaf else 0) or
                ImGuiTreeNodeFlags.OpenOnArrow or ImGuiTreeNodeFlags.SpanAvailWidth

        ImGui.pushID(entity.entity.id)
        val opened = ImGui.treeNodeEx(name, flags)
        if (ImGui.isItemClicked()) {
            scene.selectedEntity = entity
        }

        if (opened) {
            hierarchy.children.forEach { childFleksEntity ->
                drawEntityNode(scene.getEntity(childFleksEntity))
            }
            ImGui.treePop()
        }
        ImGui.popID()
    }
}
