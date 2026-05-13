package github.businessdirt.axite.vanadium.scene.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
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
