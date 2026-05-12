package github.businessdirt.axite.vanadium.vulkan.pipeline

import github.businessdirt.axite.vanadium.assets.types.Shader
import org.lwjgl.vulkan.VkDevice

/**
 * Represents a Vulkan raytracing pipeline.
 */
class RaytracingPipeline(
    device: VkDevice,
    shader: Shader, // add later
) : Pipeline(device) {

    // Raytracing pipeline implementation would go here.
    // It typically involves shader groups and a shader binding table (SBT).

    override val layout: PipelineLayout
        get() = TODO("Not yet implemented")

    override val handle: Long
        get() = TODO("Not yet implemented")
}
