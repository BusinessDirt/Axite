package github.businessdirt.axite.vanadium.scene.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import org.joml.Matrix4f

data class CameraComponent(
    var type: CameraType = CameraType.Perspective(),
    val viewMatrix: Matrix4f = Matrix4f(),
    val projectionMatrix: Matrix4f = Matrix4f(),
    val combinedMatrix: Matrix4f = Matrix4f()
) : Component<CameraComponent> {
    override fun type() = CameraComponent
    companion object : ComponentType<CameraComponent>()
}

sealed class CameraType {

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
    }
}