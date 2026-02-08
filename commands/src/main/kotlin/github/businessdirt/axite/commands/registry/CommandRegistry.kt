package github.businessdirt.axite.commands.registry

import github.businessdirt.axite.commands.Command
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.reflect.KClass

/**
 * Singleton registry for automatically discovering and registering commands.
 * Uses [ServiceLoader] to find [CommandRegistryProvider] implementations.
 */
object CommandRegistry {

    private val logger: Logger = LoggerFactory.getLogger(CommandRegistry::class.java)

    /**
     * Initializes the registry by loading command modules from the classpath.
     */
    @Suppress("UNCHECKED_CAST", "unused")
    fun initialize() = try {
        val loader = ServiceLoader.load(CommandRegistryProvider::class.java)
        loader.forEach { registry ->
            registry.modules.forEach {
                register(it as KClass<out Command<*>>)
            }
        }

        logger.info("Initialized PacketRegistry with {} packets", loader.sumOf { it.modules.size })
    } catch (e: Exception) {
        logger.error("Failed to load packet registry", e)
    }

    private fun register(command: KClass<out Command<*>>) {

    }
}
