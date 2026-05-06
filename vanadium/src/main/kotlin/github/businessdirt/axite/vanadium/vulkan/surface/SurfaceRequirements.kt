package github.businessdirt.axite.vanadium.vulkan.surface

import org.lwjgl.vulkan.KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR
import org.lwjgl.vulkan.VK13.VK_FORMAT_B8G8R8A8_SRGB
import org.lwjgl.vulkan.VkSurfaceFormatKHR

/**
 * Annotation to mark a function as a surface requirement.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SurfaceRequirement(
    val weight: Int = 0,
    val mandatory: Boolean = false
)

/**
 * Default requirements for surface format selection.
 */
object DefaultSurfaceRequirements {

    @SurfaceRequirement(weight = 1000)
    fun isB8G8R8A8(format: VkSurfaceFormatKHR) = format.format() == VK_FORMAT_B8G8R8A8_SRGB

    @SurfaceRequirement(weight = 500)
    fun isSrgbColorSpace(format: VkSurfaceFormatKHR) = format.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR
}