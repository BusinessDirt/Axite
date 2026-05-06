package github.businessdirt.axite.vanadium.vulkan.device

import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions

/**
 * Registry and evaluator for physical device requirements.
 */
object PhysicalDeviceSelector {
    private val providers = mutableListOf<Any>(DefaultPhysicalDeviceRequirements)

    /**
     * Registers a new requirement provider. All functions in the provider annotated with
     * @PhysicalDeviceRequirement will be used during device selection.
     */
    fun registerProvider(provider: Any) {
        providers.add(provider)
    }

    internal data class Requirement(
        val name: String,
        val weight: Int,
        val mandatory: Boolean,
        val message: String,
        val check: (PhysicalDevice) -> Any
    )

    internal fun getRequirements(): List<Requirement> = providers.flatMap { provider -> provider::class.memberFunctions
        .filter { it.hasAnnotation<PhysicalDeviceRequirement>() }
        .map { func ->
            val annotation = func.findAnnotation<PhysicalDeviceRequirement>()!!
            Requirement(
                name = func.name,
                weight = annotation.weight,
                mandatory = annotation.mandatory,
                message = annotation.message,
                check = { device -> func.call(provider, device)!! }
            )
        }
    }
}
