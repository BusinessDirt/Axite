package github.businessdirt.axite.vanadium.scene.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import github.businessdirt.axite.vanadium.core.imgui.ImGuiDrawable
import imgui.ImGui

data class HierarchyComponent(
    var parent: Entity? = null,
    val children: MutableSet<Entity> = mutableSetOf()
) : Component<HierarchyComponent>, ImGuiDrawable {
    companion object : ComponentType<HierarchyComponent>()

    override fun type() = HierarchyComponent

    override fun draw() {
        ImGui.text("Parent: ${parent?.id ?: "None"}")
        ImGui.text("Children: ${children.size}")
    }
}