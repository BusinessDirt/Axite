package github.businessdirt.axite.vanadium.scene

import com.github.quillraven.fleks.*
import org.joml.Matrix4f

/**
 * Manages the Fleks ECS world and provides an interface for entity creation and hierarchy management.
 */
class SceneGraph : AutoCloseable {

    private val world = configureWorld {
        systems {
            add(TransformSystem())
        }
    }

    /**
     * Creates a new entity with a [NameComponent] and a [TransformComponent].
     */
    fun createEntity(name: String = "Entity"): SceneEntity {
        val entity = world.entity {
            it += NameComponent(name)
            it += TransformComponent()
            it += HierarchyComponent()
        }
        return SceneEntity(entity, world)
    }

    /**
     * Updates the ECS world.
     */
    fun update(deltaTime: Float) {
        world.update(deltaTime)
    }

    /**
     * Iterates over all entities that have both a [TransformComponent] and a [ModelComponent].
     */
    fun forEachModel(block: (TransformComponent, ModelComponent) -> Unit) {
        with(world) {
            val family = world.family { all(TransformComponent, ModelComponent) }
            family.forEach { entity ->
                block(entity[TransformComponent], entity[ModelComponent])
            }
        }
    }

    override fun close() {
        world.dispose()
    }

    /**
     * Internal system to update global transform matrices.
     */
    private inner class TransformSystem : IteratingSystem(
        world.family { all(TransformComponent, HierarchyComponent) }
    ) {
        override fun onTickEntity(entity: Entity) {
            val hierarchy = entity[HierarchyComponent]
            
            // We only trigger calculation from root nodes (no parent)
            // The recursive call will handle children.
            if (hierarchy.parent == null) {
                updateTransform(entity, Matrix4f())
            }
        }

        private fun updateTransform(entity: Entity, parentMatrix: Matrix4f) {
            val transform = entity[TransformComponent]
            val hierarchy = entity[HierarchyComponent]

            // Calculate local matrix
            transform.localMatrix.translationRotateScale(
                transform.position,
                transform.rotation,
                transform.scale
            )

            // Calculate global matrix
            parentMatrix.mul(transform.localMatrix, transform.globalMatrix)

            // Update children
            hierarchy.children.forEach { child ->
                updateTransform(child, transform.globalMatrix)
            }
        }
    }
}
