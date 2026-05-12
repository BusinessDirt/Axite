package github.businessdirt.axite.vanadium.vulkan.pipeline

import github.businessdirt.axite.vanadium.assets.metadata.PushConstantRange
import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.Handle
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo
import org.lwjgl.vulkan.VkPushConstantRange

class PipelineLayout(
    val device: VkDevice,
    ranges: List<PushConstantRange>,
    val descriptorSetLayouts: List<DescriptorSetLayout> = emptyList()
) : Handle<Long>() {

    override val handle: Long = memoryStack { stack ->
        val layoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack).`sType$Default`()

        if (ranges.isNotEmpty()) {
            val pushConstants = VkPushConstantRange.calloc(ranges.size, stack)
            ranges.forEachIndexed { i, range ->
                pushConstants[i].stageFlags(range.stageFlags).offset(range.offset).size(range.size)
            }

            layoutCreateInfo.pPushConstantRanges(pushConstants)
        }

        if (descriptorSetLayouts.isNotEmpty()) {
            val layouts = stack.longs(*descriptorSetLayouts.map { it.handle }.toLongArray())
            layoutCreateInfo.pSetLayouts(layouts)
        }

        stack.createHandle({ "Failed to create pipeline layout" }) {
            vkCreatePipelineLayout(device, layoutCreateInfo, null, it)
        }
    }

    override fun destroy() {
        descriptorSetLayouts.forEach { it.close() }
        vkDestroyPipelineLayout(device, handle, null)
    }
}