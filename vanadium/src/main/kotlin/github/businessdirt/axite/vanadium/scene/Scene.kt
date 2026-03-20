package github.businessdirt.axite.vanadium.scene

import github.businessdirt.axite.vanadium.VanadiumConfig
import github.businessdirt.axite.vanadium.platform.Window

class Scene(
    window: Window,
    config: VanadiumConfig
) {

    val entities: MutableList<Entity> = mutableListOf()
    val projection: Projection = Projection(config.fov, config.zNear, config.zFar, window.width, window.height)

    fun addEntity(entity: Entity) = entities.add(entity)
    fun removeAllEntities() = entities.clear()
    fun removeEntity(entity: Entity) = entities.removeIf {
            entity1: Entity -> entity1.id == entity.id
    }
}