package github.businessdirt.axite.vanadium.vulkan.pipeline

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.assets.types.Shader
import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.VulkanDsl
import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer
import github.businessdirt.axite.vanadium.vulkan.descriptors.DescriptorSetLayout
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*

@VulkanDsl
class GraphicsPipelineConfiguration {
    lateinit var vertexShader: Shader
    lateinit var fragmentShader: Shader

    var topology: Int = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST
    var polygonMode: Int = VK_POLYGON_MODE_FILL
    var cullMode: Int = VK_CULL_MODE_NONE
    var frontFace: Int = VK_FRONT_FACE_CLOCKWISE
    var lineWidth: Float = 1.0f

    var enableBlend: Boolean = false

    var depthTestEnable: Boolean = true
    var depthWriteEnable: Boolean = true
    var depthCompareOp: Int = VK_COMPARE_OP_LESS_OR_EQUAL

    val specializationConstants = mutableMapOf<String, Any>()

    fun vertexShader(shader: Shader) {
        this.vertexShader = shader
    }

    fun fragmentShader(shader: Shader) {
        this.fragmentShader = shader
    }

    fun specializationConstant(name: String, value: Any) {
        this.specializationConstants[name] = value
    }
}

/**
 * Represents a Vulkan graphics pipeline.
 */
class GraphicsPipeline(
    device: VkDevice,
    val configuration: GraphicsPipelineConfiguration
) : Pipeline(device) {

    constructor(device: VkDevice, block: GraphicsPipelineConfiguration.() -> Unit) : this(device, GraphicsPipelineConfiguration().apply(block))

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

        stages[0].`sType$Default`()
            .stage(vertexShader.stage.vulkan)
            .module(vertexShader.module.handle)
            .pName(mainName)
            .pSpecializationInfo(specializationInfo(vertexShader, configuration.specializationConstants))

        stages[1].`sType$Default`()
            .stage(fragmentShader.stage.vulkan)
            .module(fragmentShader.module.handle)
            .pName(mainName)
            .pSpecializationInfo(specializationInfo(fragmentShader, configuration.specializationConstants))

        return stages
    }

    private fun MemoryStack.inputAssemblyStateCreateInfo(): VkPipelineInputAssemblyStateCreateInfo =
        VkPipelineInputAssemblyStateCreateInfo.calloc(this).`sType$Default`()
            .topology(configuration.topology)

    private fun MemoryStack.viewportStateCreateInfo(): VkPipelineViewportStateCreateInfo =
        VkPipelineViewportStateCreateInfo.calloc(this).`sType$Default`()
            .viewportCount(1).scissorCount(1)

    private fun MemoryStack.rasterizationStateCreateInfo(): VkPipelineRasterizationStateCreateInfo =
        VkPipelineRasterizationStateCreateInfo.calloc(this).`sType$Default`()
            .polygonMode(configuration.polygonMode)
            .cullMode(configuration.cullMode)
            .frontFace(configuration.frontFace)
            .lineWidth(configuration.lineWidth)

    private fun MemoryStack.multisampleStateCreateInfo(): VkPipelineMultisampleStateCreateInfo =
        VkPipelineMultisampleStateCreateInfo.calloc(this).`sType$Default`()
            .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)

    private fun MemoryStack.dynamicStateCreateInfo(): VkPipelineDynamicStateCreateInfo =
        VkPipelineDynamicStateCreateInfo.calloc(this).`sType$Default`()
            .pDynamicStates(this.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR))

    private fun MemoryStack.colorBlendAttachmentStateCreateInfo(enableBlend: Boolean): VkPipelineColorBlendStateCreateInfo {
        val attachmentState = VkPipelineColorBlendAttachmentState.calloc(1, this)
            .colorWriteMask(VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT)
            .blendEnable(enableBlend)

        if (enableBlend) attachmentState[0].colorBlendOp(VK_BLEND_OP_ADD)
            .alphaBlendOp(VK_BLEND_OP_ADD)
            .srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
            .dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
            .srcAlphaBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
            .dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)

        return VkPipelineColorBlendStateCreateInfo.calloc(this).`sType$Default`().pAttachments(attachmentState)
    }

    private fun MemoryStack.depthStencilStateCreateInfo() = VkPipelineDepthStencilStateCreateInfo.calloc(this).`sType$Default`()
        .depthTestEnable(configuration.depthTestEnable)
        .depthWriteEnable(configuration.depthWriteEnable)
        .depthCompareOp(configuration.depthCompareOp)
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
        mergePushConstants(configuration.vertexShader.metadata.pushConstantRanges + configuration.fragmentShader.metadata.pushConstantRanges),
        mergeLayoutBindings(configuration.vertexShader.metadata.layoutBindings + configuration.fragmentShader.metadata.layoutBindings).let { bindings ->
            if (bindings.isEmpty()) return@let emptyList()

            val maxSet = bindings.maxOf { it.set }
            val layouts = mutableListOf<DescriptorSetLayout>()

            for (setIndex in 0..maxSet) {
                val setBindings = bindings.filter { it.set == setIndex }
                layouts.add(DescriptorSetLayout(device, setBindings))
            }
            layouts
        }
    )

    override fun bind(commandBuffer: CommandBuffer) =
        vkCmdBindPipeline(commandBuffer.handle, VK_PIPELINE_BIND_POINT_GRAPHICS, handle)

    override val handle: Long = memoryStack { stack ->
        val pipelineCreateInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack).`sType$Default`()
            .pNext(stack.renderingCreateInfo().address())
            .renderPass(VK_NULL_HANDLE)
            .pStages(stack.shaderStageCreateInfo(configuration.vertexShader, configuration.fragmentShader))
            .pVertexInputState(stack.vertexInputStateCreateInfo(configuration.vertexShader))
            .pInputAssemblyState(stack.inputAssemblyStateCreateInfo())
            .pViewportState(stack.viewportStateCreateInfo())
            .pRasterizationState(stack.rasterizationStateCreateInfo())
            .pColorBlendState(stack.colorBlendAttachmentStateCreateInfo(configuration.enableBlend))
            .pMultisampleState(stack.multisampleStateCreateInfo())
            .pDynamicState(stack.dynamicStateCreateInfo())
            .pDepthStencilState(stack.depthStencilStateCreateInfo())
            .layout(layout.handle)

        stack.createHandle({ "Error creating graphics pipeline" }) {
            vkCreateGraphicsPipelines(device, Vanadium.context.pipelineCache.handle, pipelineCreateInfo, null, it)
        }
    }
}
