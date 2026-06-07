package github.businessdirt.axite.vanadium.scene.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import github.businessdirt.axite.vanadium.core.imgui.ImGuiDrawable
import imgui.ImGui
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f

data class TransformComponent(
    val position: Vector3f = Vector3f(),
    val rotation: Quaternionf = Quaternionf(),
    val scale: Vector3f = Vector3f(1f, 1f, 1f),
    val localMatrix: Matrix4f = Matrix4f(),
    val globalMatrix: Matrix4f = Matrix4f()
) : Component<TransformComponent>, ImGuiDrawable {
    companion object : ComponentType<TransformComponent>()

    override fun type() = TransformComponent

    override fun draw() {
        val pos = floatArrayOf(position.x, position.y, position.z)
        if (ImGui.dragFloat3("Position", pos, 0.1f)) {
            position.set(pos[0], pos[1], pos[2])
        }

        val euler = Vector3f()
        rotation.getEulerAnglesXYZ(euler)
        val rot = floatArrayOf(Math.toDegrees(euler.x.toDouble()).toFloat(), Math.toDegrees(euler.y.toDouble()).toFloat(), Math.toDegrees(euler.z.toDouble()).toFloat())
        if (ImGui.dragFloat3("Rotation", rot, 0.1f)) {
            rotation.rotationXYZ(Math.toRadians(rot[0].toDouble()).toFloat(), Math.toRadians(rot[1].toDouble()).toFloat(), Math.toRadians(rot[2].toDouble()).toFloat())
        }

        val sca = floatArrayOf(scale.x, scale.y, scale.z)
        if (ImGui.dragFloat3("Scale", sca, 0.1f)) {
            scale.set(sca[0], sca[1], sca[2])
        }
    }
}
