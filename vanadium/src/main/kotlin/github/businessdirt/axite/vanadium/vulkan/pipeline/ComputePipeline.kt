package github.businessdirt.axite.vanadium.vulkan.pipeline

import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.resources.ShaderModule
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*

/**
 * Builder for a [ComputePipeline].
 */
class ComputePipelineBuilder {
    var shaderModule: ShaderModule? = null
    var pushConstantRanges = mutableListOf<PushConstantRange>()

    fun shader(module: ShaderModule) {
        shaderModule = module
    }

    fun pushConstantRanges(vararg ranges: PushConstantRange) {
        pushConstantRanges.addAll(ranges)
    }
}

/**
 * Represents a Vulkan compute pipeline.
 */
class ComputePipeline(
    device: VkDevice,
    layoutHandle: Long,
    handle: Long
) : Pipeline(device, layoutHandle, handle) {

    companion object {
        fun create(
            device: VkDevice,
            pipelineCache: Long,
            block: ComputePipelineBuilder.(MemoryStack) -> Unit
        ): ComputePipeline {
            var layoutHandle = NULL
            var pipelineHandle = NULL

            try {
                memoryStack { stack ->
                    val builder = ComputePipelineBuilder().apply { block(stack) }
                    validateBuilder(builder)

                    layoutHandle = createPipelineLayout(device, stack, builder.pushConstantRanges)
                    pipelineHandle = createComputePipeline(device, stack, builder, layoutHandle, pipelineCache)
                }
                return ComputePipeline(device, layoutHandle, pipelineHandle)
            } catch (e: Exception) {
                if (layoutHandle != NULL) vkDestroyPipelineLayout(device, layoutHandle, null)
                if (pipelineHandle != NULL) vkDestroyPipeline(device, pipelineHandle, null)
                throw e
            }
        }

        private fun validateBuilder(builder: ComputePipelineBuilder) {
            requireNotNull(builder.shaderModule) { "Shader module is required for compute pipeline" }
        }

        private fun createComputePipeline(
            device: VkDevice,
            stack: MemoryStack,
            builder: ComputePipelineBuilder,
            layoutHandle: Long,
            pipelineCache: Long
        ): Long {
            val shaderStage = VkPipelineShaderStageCreateInfo.calloc(stack).`sType$Default`()
                .stage(VK_SHADER_STAGE_COMPUTE_BIT)
                .module(builder.shaderModule!!.handle)
                .pName(stack.UTF8("main"))

            val pipelineCreateInfo = VkComputePipelineCreateInfo.calloc(1, stack).`sType$Default`()
                .stage(shaderStage)
                .layout(layoutHandle)

            return stack.createHandle({ "Error creating compute pipeline" }) {
                vkCreateComputePipelines(device, pipelineCache, pipelineCreateInfo, null, it)
            }
        }
    }
}
