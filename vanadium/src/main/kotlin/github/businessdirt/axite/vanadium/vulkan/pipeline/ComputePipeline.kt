package github.businessdirt.axite.vanadium.vulkan.pipeline

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.assets.types.Shader
import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.VulkanDsl
import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer
import github.businessdirt.axite.vanadium.vulkan.descriptors.DescriptorSetLayout
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkComputePipelineCreateInfo
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo

@VulkanDsl
class ComputePipelineConfiguration {
    lateinit var shader: Shader
    val specializationConstants = mutableMapOf<String, Any>()

    fun shader(shader: Shader) {
        this.shader = shader
    }

    fun specializationConstant(name: String, value: Any) {
        this.specializationConstants[name] = value
    }
}

class ComputePipeline(
    device: VkDevice,
    val configuration: ComputePipelineConfiguration
) : Pipeline(device) {

    constructor(device: VkDevice, block: ComputePipelineConfiguration.() -> Unit) : this(device, ComputePipelineConfiguration().apply(block))

    constructor(block: ComputePipelineConfiguration.() -> Unit) : this(Vanadium.context.device.handle, block)

    override val layout: PipelineLayout = PipelineLayout(
        device,
        configuration.shader.metadata.pushConstantRanges,
        if (configuration.shader.metadata.layoutBindings.isNotEmpty()) {
            listOf(DescriptorSetLayout(device, configuration.shader.metadata.layoutBindings))
        } else {
            emptyList()
        }
    )

    override val handle: Long = memoryStack { stack ->
        val pipelineCreateInfo = VkComputePipelineCreateInfo.calloc(1, stack).`sType$Default`()
            .stage(stack.shaderStageCreateInfo(configuration.shader))
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
            .pSpecializationInfo(specializationInfo(shader, configuration.specializationConstants))

    override fun bind(commandBuffer: CommandBuffer) =
        vkCmdBindPipeline(commandBuffer.handle, VK_PIPELINE_BIND_POINT_COMPUTE, handle)
}
