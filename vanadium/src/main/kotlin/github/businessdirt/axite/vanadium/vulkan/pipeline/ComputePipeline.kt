package github.businessdirt.axite.vanadium.vulkan.pipeline

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.assets.types.Shader
import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK13.VK_SHADER_STAGE_COMPUTE_BIT
import org.lwjgl.vulkan.VK13.vkCreateComputePipelines
import org.lwjgl.vulkan.VkComputePipelineCreateInfo
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo

class ComputePipeline(
    device: VkDevice,
    shader: Shader
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
}
