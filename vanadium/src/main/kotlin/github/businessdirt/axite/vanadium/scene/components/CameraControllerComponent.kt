package github.businessdirt.axite.vanadium.scene.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import github.businessdirt.axite.vanadium.scene.SceneEntity

data class CameraControllerComponent(
    var settings: ControllerSettings = ControllerSettings.FirstPerson()
) : Component<CameraControllerComponent> {
    override fun type() = CameraControllerComponent
    companion object : ComponentType<CameraControllerComponent>()
}

sealed class ControllerSettings {
    abstract fun update(transform: TransformComponent, deltaTime: Float)

    data class FirstPerson(
        var sensitivity: Float = 0.1f,
        var speed: Float = 5f,
        var pitch: Float = 0f,
        var yaw: Float = 0f
    ) : ControllerSettings() {
        override fun update(transform: TransformComponent, deltaTime: Float) {

        }
    }

    data class ThirdPerson(
        var target: SceneEntity? = null,
        var distance: Float = 5f,
        var orbitSpeed: Float = 1.0f
    ) : ControllerSettings() {
        override fun update(transform: TransformComponent, deltaTime: Float) {

        }
    }

    object FreeFly : ControllerSettings() {
        override fun update(transform: TransformComponent, deltaTime: Float) {
            // Independent movement logic (Ghost cam)
        }
    }
}