package github.businessdirt.axite.vanadium.platform.vulkan.pipeline

import github.businessdirt.axite.vanadium.platform.vulkan.Context
import github.businessdirt.axite.vanadium.platform.vulkan.VulkanHandle
import github.businessdirt.axite.vanadium.utils.createHandle
import github.businessdirt.axite.vanadium.utils.memoryStack
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo
import org.lwjgl.vulkan.VkPipelineRenderingCreateInfo
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo

class PipelineBuilder {
    val shaderModules = mutableListOf<ShaderModule>()
    var vertexInputState: VkPipelineVertexInputStateCreateInfo? = null
    var colorFormat: Int = VK_FORMAT_UNDEFINED

    fun shaders(vararg modules: ShaderModule) {
        shaderModules.addAll(modules)
    }
}

class Pipeline(
     block: PipelineBuilder.(MemoryStack) -> Unit
) : VulkanHandle<Long>() {

    val layoutHandle: Long = memoryStack { stack ->
        val layoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack).`sType$Default`()
        stack.createHandle({ "Failed to create pipeline layout" }) {
            vkCreatePipelineLayout(Context.device.handle, layoutCreateInfo, null, it)
        }
    }

    override val handle: Long =  memoryStack { stack ->
        val builder = PipelineBuilder()
        builder.block(stack)

        require(builder.shaderModules.isNotEmpty()) { "At least one shader module is required" }
        requireNotNull(builder.vertexInputState) { "Vertex input state is required" }
        require(builder.colorFormat != VK_FORMAT_UNDEFINED) { "Color format is required" }

        val mainName = stack.UTF8("main")

        val shaderStages = VkPipelineShaderStageCreateInfo.calloc(builder.shaderModules.size, stack)
        builder.shaderModules.forEachIndexed { i, module ->
            shaderStages[i]
                .`sType$Default`()
                .stage(module.shaderStage)
                .module(module.handle)
                .pName(mainName)
        }

        val assemblyState = VkPipelineInputAssemblyStateCreateInfo.calloc(stack).`sType$Default`()
            .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)

        val viewportState = VkPipelineViewportStateCreateInfo.calloc(stack).`sType$Default`()
            .viewportCount(1)
            .scissorCount(1)

        val rasterizationState = VkPipelineRasterizationStateCreateInfo.calloc(stack).`sType$Default`()
            .polygonMode(VK_POLYGON_MODE_FILL)
            .cullMode(VK_CULL_MODE_NONE)
            .frontFace(VK_FRONT_FACE_CLOCKWISE)
            .lineWidth(1.0f)

        val multisampleState = VkPipelineMultisampleStateCreateInfo.calloc(stack).`sType$Default`()
            .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)

        val dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack).`sType$Default`()
            .pDynamicStates(stack.ints(
                VK_DYNAMIC_STATE_VIEWPORT,
                VK_DYNAMIC_STATE_SCISSOR
            ))

        val blendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack)
            .colorWriteMask(
                VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or
                        VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT
            )
            .blendEnable(false)

        val colorBlendState = VkPipelineColorBlendStateCreateInfo.calloc(stack).`sType$Default`()
            .pAttachments(blendAttachment)

        val renderingInfo = VkPipelineRenderingCreateInfo.calloc(stack).`sType$Default`()
            .colorAttachmentCount(1)
            .pColorAttachmentFormats(stack.ints(builder.colorFormat))

        val pipelineCreateInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack).`sType$Default`()
            .pNext(renderingInfo.address())
            .renderPass(VK_NULL_HANDLE)
            .pStages(shaderStages)
            .pVertexInputState(builder.vertexInputState)
            .pInputAssemblyState(assemblyState)
            .pViewportState(viewportState)
            .pRasterizationState(rasterizationState)
            .pColorBlendState(colorBlendState)
            .pMultisampleState(multisampleState)
            .pDynamicState(dynamicState)
            .layout(layoutHandle)

        stack.createHandle({ "Error creating graphics pipeline" }) {
            vkCreateGraphicsPipelines(Context.device.handle, Context.pipelineCache.handle, pipelineCreateInfo, null, it)
        }
    }


    override fun destroy() {
        val device = Context.device.handle
        vkDestroyPipelineLayout(device, layoutHandle, null)
        vkDestroyPipeline(device, handle, null)
    }
}