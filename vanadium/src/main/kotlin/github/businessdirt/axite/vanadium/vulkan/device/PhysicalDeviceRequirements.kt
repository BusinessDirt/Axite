package github.businessdirt.axite.vanadium.vulkan.device

import org.lwjgl.vulkan.KHRAccelerationStructure
import org.lwjgl.vulkan.KHRDeferredHostOperations
import org.lwjgl.vulkan.KHRRayTracingPipeline
import org.lwjgl.vulkan.VK13.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU

/**
 * Annotation to mark a function as a physical device requirement.
 * Requirements can be mandatory (used for filtering) or weighted (used for scoring).
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PhysicalDeviceRequirement(
    val weight: Int = 0,
    val mandatory: Boolean = false,
    val message: String = ""
)

/**
 * Default requirements for physical device selection.
 */
object DefaultPhysicalDeviceRequirements {

    @PhysicalDeviceRequirement(mandatory = true, message = "Graphics Queue missing")
    fun hasGraphicsQueue(device: PhysicalDevice) = device.hasGraphicsQueueFamily

    @PhysicalDeviceRequirement(mandatory = true, message = "Compute Queue missing")
    fun hasComputeQueue(device: PhysicalDevice) = device.hasComputeQueueFamily

    @PhysicalDeviceRequirement(mandatory = true, message = "Presentation Support missing")
    fun hasPresentationQueue(device: PhysicalDevice) = device.hasPresentationSupport

    @PhysicalDeviceRequirement(mandatory = true, message = "Required Extensions missing")
    fun supportsExtensions(device: PhysicalDevice) = device.supportsExtensions(PhysicalDevice.REQUIRED_EXTENSIONS)

    @PhysicalDeviceRequirement(weight = 1000)
    fun isDiscreteGpu(device: PhysicalDevice) =
        device.properties.properties().deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU

    @PhysicalDeviceRequirement(weight = 1)
    fun maxImageDimension(device: PhysicalDevice) =
        device.properties.properties().limits().maxImageDimension2D()
}

/**
 * Requirements for Raytracing support.
 */
object RaytracingPhysicalDeviceRequirements {

    private val RT_EXTENSIONS = setOf(
        KHRAccelerationStructure.VK_KHR_ACCELERATION_STRUCTURE_EXTENSION_NAME,
        KHRRayTracingPipeline.VK_KHR_RAY_TRACING_PIPELINE_EXTENSION_NAME,
        KHRDeferredHostOperations.VK_KHR_DEFERRED_HOST_OPERATIONS_EXTENSION_NAME
    )

    @PhysicalDeviceRequirement(mandatory = true, message = "Raytracing Extensions missing")
    fun supportsRaytracingExtensions(device: PhysicalDevice) = device.supportsExtensions(RT_EXTENSIONS)
}
