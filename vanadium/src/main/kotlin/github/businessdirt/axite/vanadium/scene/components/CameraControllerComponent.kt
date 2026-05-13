package github.businessdirt.axite.vanadium.scene.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity

data class CameraControllerComponent(
    var settings: ControllerSettings = ControllerSettings.FirstPerson()
) : Component<CameraControllerComponent> {
    override fun type() = CameraControllerComponent
    companion object : ComponentType<CameraControllerComponent>()
}

sealed class ControllerSettings {
    data class FirstPerson(
        var sensitivity: Float = 0.1f,
        var pitch: Float = 0f,
        var yaw: Float = 0f
    ) : ControllerSettings()

    data class ThirdPerson(
        var targetEntity: Entity? = null,
        var distance: Float = 5f,
        var angle: Float = 0f
    ) : ControllerSettings()

    object FreeFly : ControllerSettings()
}