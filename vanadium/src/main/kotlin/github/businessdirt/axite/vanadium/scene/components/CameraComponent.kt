package github.businessdirt.axite.vanadium.scene.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import github.businessdirt.axite.vanadium.core.imgui.ImGuiDrawable
import imgui.ImGui
import org.joml.Matrix4f

data class CameraComponent(
    var type: CameraType = CameraType.Perspective(),
    val viewMatrix: Matrix4f = Matrix4f(),
    val projectionMatrix: Matrix4f = Matrix4f(),
    val combinedMatrix: Matrix4f = Matrix4f()
) : Component<CameraComponent>, ImGuiDrawable {
    override fun type() = CameraComponent
    companion object : ComponentType<CameraComponent>()

    override fun draw() {
        if (ImGui.beginCombo("Type", type.javaClass.simpleName)) {
            if (ImGui.selectable("Perspective", type is CameraType.Perspective)) type = CameraType.Perspective()
            if (ImGui.selectable("Orthographic", type is CameraType.Orthographic)) type = CameraType.Orthographic()
            ImGui.endCombo()
        }

        type.draw()
    }
}

sealed class CameraType : ImGuiDrawable {

    abstract fun updateProjection(projectionMatrix: Matrix4f, aspectRatio: Float)

    data class Perspective(
        var fov: Float = 45f,
        var near: Float = 0.1f,
        var far: Float = 1000f
    ) : CameraType() {
        override fun updateProjection(projectionMatrix: Matrix4f, aspectRatio: Float) {
            projectionMatrix.setPerspective(Math.toRadians(fov.toDouble()).toFloat(), aspectRatio, near, far)
            projectionMatrix.m11(projectionMatrix.m11() * -1f)
        }

        override fun draw() {
            val fovArr = floatArrayOf(fov)
            if (ImGui.dragFloat("FOV", fovArr, 1f, 1f, 179f)) fov = fovArr[0]

            val nearArr = floatArrayOf(near)
            if (ImGui.dragFloat("Near", nearArr, 0.1f, 0.01f, far - 0.1f)) near = nearArr[0]

            val farArr = floatArrayOf(far)
            if (ImGui.dragFloat("Far", farArr, 1f, near + 0.1f, 10000f)) far = farArr[0]
        }
    }

    data class Orthographic(
        var zoom: Float = 1f,
        var near: Float = -1f,
        var far: Float = 1f
    ) : CameraType() {
        override fun updateProjection(projectionMatrix: Matrix4f, aspectRatio: Float) {
            projectionMatrix.setOrtho(-zoom * aspectRatio, zoom * aspectRatio, -zoom, zoom, near, far)
            projectionMatrix.m11(projectionMatrix.m11() * -1f)
        }

        override fun draw() {
            val zoomArr = floatArrayOf(zoom)
            if (ImGui.dragFloat("Zoom", zoomArr, 0.1f, 0.1f, 100f)) zoom = zoomArr[0]

            val nearArr = floatArrayOf(near)
            if (ImGui.dragFloat("Near", nearArr, 0.1f)) near = nearArr[0]

            val farArr = floatArrayOf(far)
            if (ImGui.dragFloat("Far", farArr, 0.1f)) far = farArr[0]
        }
    }
}