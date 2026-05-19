package github.businessdirt.axite.vanadium.scene

import com.github.quillraven.fleks.*
import github.businessdirt.axite.vanadium.scene.components.HierarchyComponent

class Entity(
    val entity: com.github.quillraven.fleks.Entity,
    val world: World
) {

    /**
     * Adds a component to the entity.
     */
    inline fun <reified T : Component<T>> add(component: T) = with(world) {
        entity.configure { it += component }
    }

    /**
     * Removes a component from the entity.
     */
    inline fun <reified T : Component<T>> remove(type: ComponentType<T>) = with (world) {
        entity.configure { it -= type }
    }

    /**
     * Gets a component from the entity.
     */
    inline operator fun <reified T : Component<T>> get(type: ComponentType<T>): T = with(world) {
        entity[type]
    }

    /**
     * Checks if the entity has a specific component.
     */
    fun <T : Component<T>> has(type: ComponentType<T>): Boolean = with(world) {
        entity has type
    }

    /**
     * Configures the entity by adding/removing multiple components.
     */
    fun configure(block: EntityUpdateContext.(com.github.quillraven.fleks.Entity) -> Unit) = with(world) {
        entity.configure(block)
    }

    /**
     * Destroys the entity.
     */
    fun destroy() = with(world) {
        entity.remove()
    }

    /**
     * Sets the parent of this entity.
     */
    fun setParent(parent: Entity?) {
        val hierarchy = get(HierarchyComponent)

        // Remove from old parent
        hierarchy.parent?.let { oldParentEntity ->
            with(world) {
                if (oldParentEntity in world && oldParentEntity has HierarchyComponent)
                    oldParentEntity[HierarchyComponent].children.remove(entity)
            }
        }

        // Set new parent
        hierarchy.parent = parent?.entity
        parent?.get(HierarchyComponent)?.children?.add(entity)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Entity) return false
        return entity == other.entity && world == other.world
    }

    override fun hashCode(): Int = 31 * entity.id + world.hashCode()
}