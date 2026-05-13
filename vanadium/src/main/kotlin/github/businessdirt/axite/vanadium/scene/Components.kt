package github.businessdirt.axite.vanadium.scene

import github.businessdirt.axite.vanadium.assets.types.Model
import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f

data class TransformComponent(
    val position: Vector3f = Vector3f(),
    val rotation: Quaternionf = Quaternionf(),
    val scale: Vector3f = Vector3f(1f, 1f, 1f),
    val localMatrix: Matrix4f = Matrix4f(),
    val globalMatrix: Matrix4f = Matrix4f()
) : Component<TransformComponent> {
    companion object : ComponentType<TransformComponent>()

    override fun type() = TransformComponent
}

data class ModelComponent(
    var model: Model? = null
) : Component<ModelComponent> {
    companion object : ComponentType<ModelComponent>()

    override fun type() = ModelComponent
}

data class NameComponent(
    var name: String = "Entity"
) : Component<NameComponent> {
    companion object : ComponentType<NameComponent>()

    override fun type() = NameComponent
}

data class HierarchyComponent(
    var parent: Entity? = null,
    val children: MutableSet<Entity> = mutableSetOf()
) : Component<HierarchyComponent> {
    companion object : ComponentType<HierarchyComponent>()

    override fun type() = HierarchyComponent
}
