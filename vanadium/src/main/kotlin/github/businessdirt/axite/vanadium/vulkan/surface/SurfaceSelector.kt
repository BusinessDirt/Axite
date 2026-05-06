package github.businessdirt.axite.vanadium.vulkan.surface

import org.lwjgl.vulkan.VkSurfaceFormatKHR
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions

/**
 * Registry and evaluator for surface requirements.
 */
object SurfaceSelector {
    private val providers = mutableListOf<Any>(DefaultSurfaceRequirements)

    fun registerProvider(provider: Any) {
        providers.add(provider)
    }

    internal data class Requirement(
        val name: String,
        val weight: Int,
        val mandatory: Boolean,
        val check: (VkSurfaceFormatKHR) -> Any
    )

    internal fun getRequirements(): List<Requirement> {
        return providers.flatMap { provider ->
            provider::class.memberFunctions
                .filter { it.hasAnnotation<SurfaceRequirement>() }
                .map { func ->
                    val annotation = func.findAnnotation<SurfaceRequirement>()!!
                    Requirement(
                        name = func.name,
                        weight = annotation.weight,
                        mandatory = annotation.mandatory,
                        check = { format -> func.call(provider, format)!! }
                    )
                }
        }
    }
}
