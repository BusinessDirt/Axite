package github.businessdirt.axite.vanadium.scene.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World
import github.businessdirt.axite.vanadium.scene.components.CameraControllerComponent
import github.businessdirt.axite.vanadium.scene.components.TransformComponent

class CameraControllerSystem : IteratingSystem(
    World.family { all(CameraControllerComponent, TransformComponent) }
) {
    override fun onTickEntity(entity: Entity) {
        val controller = entity[CameraControllerComponent]
        val transform = entity[TransformComponent]
        controller.settings.update(transform, deltaTime)
    }
}