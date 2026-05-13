package github.businessdirt.axite.vanadium.scene.systems

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World
import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.scene.components.CameraComponent
import github.businessdirt.axite.vanadium.scene.components.TransformComponent
import org.joml.Vector3f

class CameraSystem : IteratingSystem(
    World.family { all(CameraComponent, TransformComponent) }
) {
    // Reuse these to avoid GC pressure
    private val direction = Vector3f()
    private val up = Vector3f()
    private val center = Vector3f()

    override fun onTickEntity(entity: Entity) {
        val cam = entity[CameraComponent]
        val transform = entity[TransformComponent]
        val aspect = Vanadium.window.data.aspectRatio

        // Calculate the Direction vectors from the orientation
        transform.rotation.transform(direction.set(0f, 0f, -1f))
        transform.rotation.transform(up.set(0f, 1f, 0f))

        // Center (Target) = Position + Direction
        transform.position.add(direction, center)

        // Update Matrices
        cam.viewMatrix.setLookAt(transform.position, center, up)
        cam.type.updateProjection(cam.projectionMatrix, aspect)

        // Combined = Projection * View
        cam.projectionMatrix.mul(cam.viewMatrix, cam.combinedMatrix)
    }
}