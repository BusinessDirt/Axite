package github.businessdirt.axite.vanadium.scene

import com.github.quillraven.fleks.configureWorld
import github.businessdirt.axite.vanadium.scene.components.HierarchyComponent
import github.businessdirt.axite.vanadium.scene.components.ModelComponent
import github.businessdirt.axite.vanadium.scene.components.NameComponent
import github.businessdirt.axite.vanadium.scene.components.TransformComponent
import github.businessdirt.axite.vanadium.scene.systems.CameraControllerSystem
import github.businessdirt.axite.vanadium.scene.systems.CameraSystem
import github.businessdirt.axite.vanadium.scene.systems.TransformSystem

class Scene : AutoCloseable {

    private val world = configureWorld {
        systems {
            add(CameraSystem())
            add(CameraControllerSystem())
            add(TransformSystem())
        }
    }

    private val modelFamily = world.family { all(TransformComponent, ModelComponent) }

    fun createEntity(name: String = "Entity"): Entity {
        val entity = world.entity {
            it += NameComponent(name)
            it += TransformComponent()
            it += HierarchyComponent()
        }
        return Entity(entity, world)
    }

    fun update(deltaTime: Float) = world.update(deltaTime)

    fun forEachModel(block: (TransformComponent, ModelComponent) -> Unit) = modelFamily.forEach { entity ->
        block(entity[TransformComponent], entity[ModelComponent])
    }

    override fun close() = world.dispose()
}