package github.businessdirt.axite.commands.registry

import kotlin.reflect.KClass

/**
 * Service provider interface for registering commands.
 * Implementations should provide a list of command classes.
 */
interface CommandRegistryProvider {
    /** The list of command classes to register. */
    val modules: List<KClass<*>>
}
