package github.businessdirt.axite.vanadium.vulkan.pipeline

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.assets.types.Shader
import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer
import github.businessdirt.axite.vanadium.vulkan.swapchain.Swapchain
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*
import kotlin.apply

/**
 * Represents a Vulkan graphics pipeline.
 */
class GraphicsPipeline(
    device: VkDevice,
    vertexShader: Shader,
    fragmentShader: Shader
) : Pipeline(device) {

    private fun MemoryStack.renderingCreateInfo(): VkPipelineRenderingCreateInfo {
        val info = VkPipelineRenderingCreateInfo.calloc(this).`sType$Default`()
            .colorAttachmentCount(1)
            .pColorAttachmentFormats(this.ints(Vanadium.context.surface.surfaceFormat.imageFormat))
        if (Vanadium.context.surface.depthFormat != VK_FORMAT_UNDEFINED) info.depthAttachmentFormat(Vanadium.context.surface.depthFormat)
        return info
    }

    private fun MemoryStack.shaderStageCreateInfo(vertexShader: Shader, fragmentShader: Shader): VkPipelineShaderStageCreateInfo.Buffer {
        val stages = VkPipelineShaderStageCreateInfo.calloc(2, this)
        val mainName = this.UTF8("main")

        stages[0].`sType$Default`().stage(vertexShader.stage.vulkan).module(vertexShader.module.handle).pName(mainName)
        stages[1].`sType$Default`().stage(fragmentShader.stage.vulkan).module(fragmentShader.module.handle)
            .pName(mainName)

        return stages
    }

    private fun MemoryStack.inputAssemblyStateCreateInfo(): VkPipelineInputAssemblyStateCreateInfo =
        VkPipelineInputAssemblyStateCreateInfo.calloc(this).`sType$Default`()
            .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)

    private fun MemoryStack.viewportStateCreateInfo(): VkPipelineViewportStateCreateInfo =
        VkPipelineViewportStateCreateInfo.calloc(this).`sType$Default`()
            .viewportCount(1).scissorCount(1)

    private fun MemoryStack.rasterizationStateCreateInfo(): VkPipelineRasterizationStateCreateInfo =
        VkPipelineRasterizationStateCreateInfo.calloc(this).`sType$Default`()
            .polygonMode(VK_POLYGON_MODE_FILL)
            .cullMode(VK_CULL_MODE_NONE)
            .frontFace(VK_FRONT_FACE_CLOCKWISE)
            .lineWidth(1.0f)

    private fun MemoryStack.multisampleStateCreateInfo(): VkPipelineMultisampleStateCreateInfo =
        VkPipelineMultisampleStateCreateInfo.calloc(this).`sType$Default`()
            .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)

    private fun MemoryStack.dynamicStateCreateInfo(): VkPipelineDynamicStateCreateInfo =
        VkPipelineDynamicStateCreateInfo.calloc(this).`sType$Default`()
            .pDynamicStates(this.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR))

    private fun MemoryStack.colorBlendAttachmentStateCreateInfo(): VkPipelineColorBlendStateCreateInfo {
        val attachment = VkPipelineColorBlendAttachmentState.calloc(1, this)
            .colorWriteMask(VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT)
            .blendEnable(false)
        return VkPipelineColorBlendStateCreateInfo.calloc(this).`sType$Default`().pAttachments(attachment)
    }

    private fun MemoryStack.depthStencilStateCreateInfo() =
        VkPipelineDepthStencilStateCreateInfo.calloc(this).`sType$Default`()
            .depthTestEnable(true)
            .depthWriteEnable(true)
            .depthCompareOp(VK_COMPARE_OP_LESS_OR_EQUAL)
            .depthBoundsTestEnable(false)
            .stencilTestEnable(false)

    private fun MemoryStack.vertexInputStateCreateInfo(vertexShader: Shader): VkPipelineVertexInputStateCreateInfo {
        val metadata = vertexShader.metadata
        val bindings = metadata.vertexInputBindings
        val attributes = metadata.vertexInputAttributes

        val info = VkPipelineVertexInputStateCreateInfo.calloc(this).`sType$Default`()

        if (bindings.isNotEmpty()) {
            val vkBindings = VkVertexInputBindingDescription.calloc(bindings.size, this)
            bindings.forEachIndexed { i, binding ->
                vkBindings[i].binding(binding.binding).stride(binding.stride).inputRate(binding.inputRate)
            }
            info.pVertexBindingDescriptions(vkBindings)
        }

        if (attributes.isNotEmpty()) {
            val vkAttributes = VkVertexInputAttributeDescription.calloc(attributes.size, this)
            attributes.forEachIndexed { i, attribute ->
                vkAttributes[i].location(attribute.location).binding(attribute.binding).format(attribute.format).offset(attribute.offset)
            }
            info.pVertexAttributeDescriptions(vkAttributes)
        }

        return info
    }

    override val layout: PipelineLayout = PipelineLayout(
        device,
        mergePushConstants(vertexShader.metadata.pushConstantRanges + fragmentShader.metadata.pushConstantRanges),
        mergeLayoutBindings(vertexShader.metadata.layoutBindings + fragmentShader.metadata.layoutBindings).let {
            if (it.isNotEmpty()) listOf(DescriptorSetLayout(device, it)) else emptyList()
        }
    )

    override fun bind(commandBuffer: CommandBuffer) =
        vkCmdBindPipeline(commandBuffer.handle, VK_PIPELINE_BIND_POINT_GRAPHICS, handle)

    override val handle: Long = memoryStack { stack ->
        val pipelineCreateInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack).`sType$Default`()
            .pNext(stack.renderingCreateInfo().address())
            .renderPass(VK_NULL_HANDLE)
            .pStages(stack.shaderStageCreateInfo(vertexShader, fragmentShader))
            .pVertexInputState(stack.vertexInputStateCreateInfo(vertexShader))
            .pInputAssemblyState(stack.inputAssemblyStateCreateInfo())
            .pViewportState(stack.viewportStateCreateInfo())
            .pRasterizationState(stack.rasterizationStateCreateInfo())
            .pColorBlendState(stack.colorBlendAttachmentStateCreateInfo())
            .pMultisampleState(stack.multisampleStateCreateInfo())
            .pDynamicState(stack.dynamicStateCreateInfo())
            .pDepthStencilState(stack.depthStencilStateCreateInfo())
            .layout(layout.handle)

        stack.createHandle({ "Error creating graphics pipeline" }) {
            vkCreateGraphicsPipelines(device, Vanadium.context.pipelineCache.handle, pipelineCreateInfo, null, it)
        }
    }

}
