package github.businessdirt.axite.commands.registry

import kotlin.reflect.KClass

interface CommandRegistryProvider {
    val modules: List<KClass<*>>
}