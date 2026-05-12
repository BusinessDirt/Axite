package github.businessdirt.axite.vanadium.vulkan.pipeline

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.assets.types.Shader
import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.swapchain.Swapchain
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*

/**
 * Represents a Vulkan graphics pipeline.
 */
class GraphicsPipeline(
    device: VkDevice,
    vertexShader: Shader,
    fragmentShader: Shader
) : Pipeline(device) {

    private val shaderStages = memoryStack { stack ->
        val stages = VkPipelineShaderStageCreateInfo.calloc(2, stack)
        val mainName = stack.UTF8("main")

        stages[0].`sType$Default`().stage(vertexShader.stage.vulkan).module(vertexShader.module.handle).pName(mainName)
        stages[1].`sType$Default`().stage(fragmentShader.stage.vulkan).module(fragmentShader.module.handle)
            .pName(mainName)

        stages
    }

    private val renderingInfo = memoryStack { stack ->
        val info = VkPipelineRenderingCreateInfo.calloc(stack).`sType$Default`()
            .colorAttachmentCount(1)
            .pColorAttachmentFormats(stack.ints(Vanadium.context.surface.surfaceFormat.imageFormat))
        if (Swapchain.DEPTH_FORMAT != VK_FORMAT_UNDEFINED) info.depthAttachmentFormat(Swapchain.DEPTH_FORMAT)
        info
    }

    private val assemblyState = memoryStack { stack ->
        VkPipelineInputAssemblyStateCreateInfo.calloc(stack).`sType$Default`()
            .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
    }

    private val viewportState = memoryStack { stack ->
        VkPipelineViewportStateCreateInfo.calloc(stack).`sType$Default`()
            .viewportCount(1).scissorCount(1)
    }

    private val rasterizationState = memoryStack { stack ->
        VkPipelineRasterizationStateCreateInfo.calloc(stack).`sType$Default`()
            .polygonMode(VK_POLYGON_MODE_FILL)
            .cullMode(VK_CULL_MODE_NONE)
            .frontFace(VK_FRONT_FACE_CLOCKWISE)
            .lineWidth(1.0f)
    }

    private val multisampleState = memoryStack { stack ->
        VkPipelineMultisampleStateCreateInfo.calloc(stack).`sType$Default`()
            .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
    }

    private val dynamicState = memoryStack { stack ->
        VkPipelineDynamicStateCreateInfo.calloc(stack).`sType$Default`()
            .pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR))
    }

    private val colorBlendState = memoryStack { stack ->
        val attachment = VkPipelineColorBlendAttachmentState.calloc(1, stack)
            .colorWriteMask(VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT)
            .blendEnable(false)
        VkPipelineColorBlendStateCreateInfo.calloc(stack).`sType$Default`().pAttachments(attachment)
    }

    private val depthStencilState = memoryStack { stack ->
        VkPipelineDepthStencilStateCreateInfo.calloc(stack).`sType$Default`()
            .depthTestEnable(true)
            .depthWriteEnable(true)
            .depthCompareOp(VK_COMPARE_OP_LESS_OR_EQUAL)
            .depthBoundsTestEnable(false)
            .stencilTestEnable(false)
    }

    private val vertexInputState = memoryStack { stack ->
        val metadata = vertexShader.metadata
        val bindings = metadata.vertexInputBindings
        val attributes = metadata.vertexInputAttributes

        val info = VkPipelineVertexInputStateCreateInfo.calloc(stack).`sType$Default`()

        if (bindings.isNotEmpty()) {
            val vkBindings = VkVertexInputBindingDescription.calloc(bindings.size, stack)
            bindings.forEachIndexed { i, binding ->
                vkBindings[i].binding(binding.binding).stride(binding.stride).inputRate(binding.inputRate)
            }
            info.pVertexBindingDescriptions(vkBindings)
        }

        if (attributes.isNotEmpty()) {
            val vkAttributes = VkVertexInputAttributeDescription.calloc(attributes.size, stack)
            attributes.forEachIndexed { i, attribute ->
                vkAttributes[i].location(attribute.location).binding(attribute.binding).format(attribute.format).offset(attribute.offset)
            }
            info.pVertexAttributeDescriptions(vkAttributes)
        }
        info
    }

    override val layout: PipelineLayout = PipelineLayout(
        device,
        mergePushConstants(vertexShader.metadata.pushConstantRanges + fragmentShader.metadata.pushConstantRanges),
        mergeLayoutBindings(vertexShader.metadata.layoutBindings + fragmentShader.metadata.layoutBindings).let {
            if (it.isNotEmpty()) listOf(DescriptorSetLayout(device, it)) else emptyList()
        }
    )

    override val handle: Long = memoryStack { stack ->
        val pipelineCreateInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack).`sType$Default`()
            .pNext(renderingInfo.address())
            .renderPass(VK_NULL_HANDLE)
            .pStages(shaderStages)
            .pVertexInputState(vertexInputState)
            .pInputAssemblyState(assemblyState)
            .pViewportState(viewportState)
            .pRasterizationState(rasterizationState)
            .pColorBlendState(colorBlendState)
            .pMultisampleState(multisampleState)
            .pDynamicState(dynamicState)
            .layout(layout.handle)

        stack.createHandle({ "Error creating graphics pipeline" }) {
            vkCreateGraphicsPipelines(device, Vanadium.context.pipelineCache.handle, pipelineCreateInfo, null, it)
        }
    }

}
