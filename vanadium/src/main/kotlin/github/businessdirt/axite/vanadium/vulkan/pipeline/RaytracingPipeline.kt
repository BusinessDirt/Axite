package github.businessdirt.axite.vanadium.vulkan.pipeline

import org.lwjgl.vulkan.VkDevice

/**
 * Represents a Vulkan raytracing pipeline.
 */
class RaytracingPipeline(
    device: VkDevice,
    layoutHandle: Long,
    handle: Long
) : Pipeline(device, layoutHandle, handle) {
    // Raytracing pipeline implementation would go here.
    // It typically involves shader groups and a shader binding table (SBT).
}

/**
 * Builder for a [RaytracingPipeline].
 */
class RaytracingPipelineBuilder {
    // Builder for raytracing pipeline
}
