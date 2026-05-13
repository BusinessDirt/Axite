package github.businessdirt.axite.vanadium.scene.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

data class NameComponent(
    var name: String = "Entity"
) : Component<NameComponent> {
    companion object : ComponentType<NameComponent>()

    override fun type() = NameComponent
}
