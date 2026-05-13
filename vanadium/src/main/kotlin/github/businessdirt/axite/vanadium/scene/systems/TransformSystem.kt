package github.businessdirt.axite.vanadium.scene.systems

import com.github.quillraven.fleks.*
import github.businessdirt.axite.vanadium.scene.components.HierarchyComponent
import github.businessdirt.axite.vanadium.scene.components.TransformComponent
import org.joml.Matrix4f

/**
 * System to update global transform matrices based on the scene hierarchy.
 */
class TransformSystem : IteratingSystem(
    World.family { all(TransformComponent, HierarchyComponent) }
) {

    override fun onTickEntity(entity: Entity) {
        val hierarchy = entity[HierarchyComponent]

        // We only trigger calculation from root nodes (no parent).
        // Root nodes use an identity matrix as the "parent" matrix.
        if (hierarchy.parent == null) {
            updateTransform(entity, Matrix4f())
        }
    }

    private fun updateTransform(entity: Entity, parentMatrix: Matrix4f) {
        val transform = entity[TransformComponent]
        val hierarchy = entity[HierarchyComponent]

        // Calculate local matrix: T * R * S
        transform.localMatrix.translationRotateScale(
            transform.position,
            transform.rotation,
            transform.scale
        )

        // Calculate global matrix: ParentGlobal * Local
        parentMatrix.mul(transform.localMatrix, transform.globalMatrix)

        // Update children recursively
        hierarchy.children.forEach { child ->
            // In Fleks, we access components of other entities via the world
            if (child in world) {
                updateTransform(child, transform.globalMatrix)
            }
        }
    }
}