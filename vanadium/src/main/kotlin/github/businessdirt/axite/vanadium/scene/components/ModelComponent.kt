package github.businessdirt.axite.vanadium.scene.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import github.businessdirt.axite.vanadium.assets.types.Model

data class ModelComponent(
    var model: Model? = null
) : Component<ModelComponent> {
    companion object : ComponentType<ModelComponent>()

    override fun type() = ModelComponent
}
