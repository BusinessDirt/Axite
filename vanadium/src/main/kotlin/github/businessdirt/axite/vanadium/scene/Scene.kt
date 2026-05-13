package github.businessdirt.axite.vanadium.scene

import com.github.quillraven.fleks.configureWorld
import github.businessdirt.axite.vanadium.scene.components.HierarchyComponent
import github.businessdirt.axite.vanadium.scene.components.ModelComponent
import github.businessdirt.axite.vanadium.scene.components.NameComponent
import github.businessdirt.axite.vanadium.scene.components.TransformComponent
import github.businessdirt.axite.vanadium.scene.systems.TransformSystem

class Scene : AutoCloseable {

    private val world = configureWorld {
        systems {
            add(TransformSystem())
        }
    }

    fun createEntity(name: String = "Entity"): SceneEntity {
        val entity = world.entity {
            it += NameComponent(name)
            it += TransformComponent()
            it += HierarchyComponent()
        }
        return SceneEntity(entity, world)
    }

    fun update(deltaTime: Float) = world.update(deltaTime)

    fun forEachModel(block: (TransformComponent, ModelComponent) -> Unit) {
        // Efficiency tip: Store the family as a property if you call this every frame
        val family = world.family { all(TransformComponent, ModelComponent) }
        family.forEach { entity ->
            block(entity[TransformComponent], entity[ModelComponent])
        }
    }

    override fun close() = world.dispose()
}