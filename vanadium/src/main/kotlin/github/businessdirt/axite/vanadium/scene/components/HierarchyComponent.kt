package github.businessdirt.axite.vanadium.scene.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity

data class HierarchyComponent(
    var parent: Entity? = null,
    val children: MutableSet<Entity> = mutableSetOf()
) : Component<HierarchyComponent> {
    companion object : ComponentType<HierarchyComponent>()

    override fun type() = HierarchyComponent
}