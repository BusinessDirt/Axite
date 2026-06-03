package github.businessdirt.axite.vanadium.vulkan.pipeline

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.assets.types.Shader
import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer
import github.businessdirt.axite.vanadium.vulkan.descriptors.DescriptorSetLayout
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkComputePipelineCreateInfo
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo

class ComputePipeline(
    device: VkDevice,
    shader: Shader,
    val specializationConstants: Map<String, Any> = emptyMap()
) : Pipeline(device) {

    override val layout: PipelineLayout = PipelineLayout(
        device,
        shader.metadata.pushConstantRanges,
        if (shader.metadata.layoutBindings.isNotEmpty()) {
            listOf(DescriptorSetLayout(device, shader.metadata.layoutBindings))
        } else {
            emptyList()
        }
    )

    override val handle: Long = memoryStack { stack ->
        val pipelineCreateInfo = VkComputePipelineCreateInfo.calloc(1, stack).`sType$Default`()
            .stage(stack.shaderStageCreateInfo(shader))
            .layout(layout.handle)

        stack.createHandle({ "Error creating compute pipeline" }) {
            vkCreateComputePipelines(device, Vanadium.context.pipelineCache.handle, pipelineCreateInfo, null, it)
        }
    }

    private fun MemoryStack.shaderStageCreateInfo(shader: Shader) =
        VkPipelineShaderStageCreateInfo.calloc(this).`sType$Default`()
            .stage(VK_SHADER_STAGE_COMPUTE_BIT)
            .module(shader.module.handle)
            .pName(this.UTF8("main"))
            .pSpecializationInfo(specializationInfo(shader, specializationConstants))

    override fun bind(commandBuffer: CommandBuffer) =
        vkCmdBindPipeline(commandBuffer.handle, VK_PIPELINE_BIND_POINT_COMPUTE, handle)
}
