package github.businessdirt.axite.vanadium.vulkan.pipeline

import github.businessdirt.axite.vanadium.assets.types.Shader
import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer
import org.lwjgl.vulkan.VkDevice

/**
 * Represents a Vulkan raytracing pipeline.
 */
class RaytracingPipeline(
    device: VkDevice,
    shader: Shader, // add later
) : Pipeline(device) {

    override val layout: PipelineLayout
        get() = TODO("Not yet implemented")

    override fun bind(commandBuffer: CommandBuffer) {
        TODO("Not yet implemented")
    }

    override val handle: Long
        get() = TODO("Not yet implemented")
}
