package github.businessdirt.axite.vanadium.platform.vulkan.pipeline

import github.businessdirt.axite.vanadium.platform.vulkan.Context
import github.businessdirt.axite.vanadium.platform.vulkan.VulkanHandle
import github.businessdirt.axite.vanadium.utils.createHandle
import github.businessdirt.axite.vanadium.utils.memoryStack
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.vkCreatePipelineLayout
import org.lwjgl.vulkan.VK13.*


data class PushConstantRange(val stage: Int, val offset: Int, val size: Int)

class PipelineBuilder {
    val shaderModules = mutableListOf<ShaderModule>()
    var vertexInputState: VkPipelineVertexInputStateCreateInfo? = null
    var colorFormat: Int = VK_FORMAT_UNDEFINED
    var depthFormat: Int = VK_FORMAT_UNDEFINED
    var pushConstantRanges = mutableListOf<PushConstantRange>()

    fun shaders(vararg modules: ShaderModule) {
        shaderModules.addAll(modules)
    }

    fun pushConstantRanges(vararg ranges: PushConstantRange) {
        pushConstantRanges.addAll(ranges)
    }
}

class Pipeline(
     block: PipelineBuilder.(MemoryStack) -> Unit
) : VulkanHandle<Long>() {

    val layoutHandle: Long
    override val handle: Long

    init {
        var tempLayout = NULL
        var tempPipeline = NULL

        memoryStack { stack ->
            // 1. Configure
            val builder = PipelineBuilder().apply { block(stack) }
            validateBuilder(builder)

            // 2. Build Layout
            tempLayout = createPipelineLayout(stack, builder)

            // 3. Build Pipeline
            tempPipeline = createGraphicsPipeline(stack, builder, tempLayout)
        }

        layoutHandle = tempLayout
        handle = tempPipeline
    }

    // ========================================================================
    // PRIVATE UGLY VULKAN BOILERPLATE BELOW THIS LINE
    // ========================================================================

    private fun validateBuilder(builder: PipelineBuilder) {
        require(builder.shaderModules.isNotEmpty()) { "At least one shader module is required" }
        requireNotNull(builder.vertexInputState) { "Vertex input state is required" }
        require(builder.colorFormat != VK_FORMAT_UNDEFINED) { "Color format is required" }
    }

    private fun createPipelineLayout(stack: MemoryStack, builder: PipelineBuilder): Long {
        val layoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack).`sType$Default`()

        if (builder.pushConstantRanges.isNotEmpty()) {
            val pushConstants = VkPushConstantRange.calloc(builder.pushConstantRanges.size, stack)
            builder.pushConstantRanges.forEachIndexed { i, range ->
                pushConstants[i].stageFlags(range.stage).offset(range.offset).size(range.size)
            }
            layoutCreateInfo.pPushConstantRanges(pushConstants)
        }

        return stack.createHandle({ "Failed to create pipeline layout" }) {
            vkCreatePipelineLayout(Context.device.handle, layoutCreateInfo, null, it)
        }
    }

    private fun createGraphicsPipeline(stack: MemoryStack, builder: PipelineBuilder, layoutHandle: Long): Long {
        val pipelineCreateInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack).`sType$Default`()
            .pNext(createRenderingInfo(stack, builder).address())
            .renderPass(VK_NULL_HANDLE)
            .pStages(createShaderStages(stack, builder))
            .pVertexInputState(builder.vertexInputState)
            .pInputAssemblyState(createAssemblyState(stack))
            .pViewportState(createViewportState(stack))
            .pRasterizationState(createRasterizationState(stack))
            .pColorBlendState(createColorBlendState(stack))
            .pMultisampleState(createMultisampleState(stack))
            .pDynamicState(createDynamicState(stack))
            .layout(layoutHandle)

        // Attach depth state only if depth format was provided
        if (builder.depthFormat != VK_FORMAT_UNDEFINED) {
            pipelineCreateInfo.pDepthStencilState(createDepthStencilState(stack))
        }

        return stack.createHandle({ "Error creating graphics pipeline" }) {
            vkCreateGraphicsPipelines(Context.device.handle, Context.pipelineCache.handle, pipelineCreateInfo, null, it)
        }
    }

    private fun createShaderStages(stack: MemoryStack, builder: PipelineBuilder): VkPipelineShaderStageCreateInfo.Buffer {
        val stages = VkPipelineShaderStageCreateInfo.calloc(builder.shaderModules.size, stack)
        val mainName = stack.UTF8("main")
        builder.shaderModules.forEachIndexed { i, module ->
            stages[i].`sType$Default`().stage(module.shaderStage).module(module.handle).pName(mainName)
        }
        return stages
    }

    private fun createRenderingInfo(stack: MemoryStack, builder: PipelineBuilder): VkPipelineRenderingCreateInfo {
        val info = VkPipelineRenderingCreateInfo.calloc(stack).`sType$Default`()
            .colorAttachmentCount(1)
            .pColorAttachmentFormats(stack.ints(builder.colorFormat))
        if (builder.depthFormat != VK_FORMAT_UNDEFINED) info.depthAttachmentFormat(builder.depthFormat)
        return info
    }

    private fun createAssemblyState(stack: MemoryStack) = VkPipelineInputAssemblyStateCreateInfo.calloc(stack).`sType$Default`()
        .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)

    private fun createViewportState(stack: MemoryStack) = VkPipelineViewportStateCreateInfo.calloc(stack).`sType$Default`()
        .viewportCount(1).scissorCount(1)

    private fun createRasterizationState(stack: MemoryStack) = VkPipelineRasterizationStateCreateInfo.calloc(stack).`sType$Default`()
        .polygonMode(VK_POLYGON_MODE_FILL).cullMode(VK_CULL_MODE_NONE).frontFace(VK_FRONT_FACE_CLOCKWISE).lineWidth(1.0f)

    private fun createMultisampleState(stack: MemoryStack) = VkPipelineMultisampleStateCreateInfo.calloc(stack).`sType$Default`()
        .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)

    private fun createDynamicState(stack: MemoryStack) = VkPipelineDynamicStateCreateInfo.calloc(stack).`sType$Default`()
        .pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR))

    private fun createColorBlendState(stack: MemoryStack): VkPipelineColorBlendStateCreateInfo {
        val attachment = VkPipelineColorBlendAttachmentState.calloc(1, stack)
            .colorWriteMask(VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT)
            .blendEnable(false)
        return VkPipelineColorBlendStateCreateInfo.calloc(stack).`sType$Default`().pAttachments(attachment)
    }

    private fun createDepthStencilState(stack: MemoryStack) = VkPipelineDepthStencilStateCreateInfo.calloc(stack).`sType$Default`()
        .depthTestEnable(true).depthWriteEnable(true).depthCompareOp(VK_COMPARE_OP_LESS_OR_EQUAL).depthBoundsTestEnable(false).stencilTestEnable(false)


    override fun destroy() {
        val device = Context.device.handle
        vkDestroyPipelineLayout(device, layoutHandle, null)
        vkDestroyPipeline(device, handle, null)
    }
}