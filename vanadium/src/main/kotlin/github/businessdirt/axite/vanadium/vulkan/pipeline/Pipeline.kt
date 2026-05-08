package github.businessdirt.axite.vanadium.vulkan.pipeline

import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.vulkan.Handle
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo
import org.lwjgl.vulkan.VkPushConstantRange
import org.lwjgl.vulkan.VK10.vkCreatePipelineLayout
import org.lwjgl.vulkan.VK10.vkDestroyPipeline
import org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout

/**
 * Represents a Vulkan push constant range.
 * @property stage The shader stage(s) that will use the push constants.
 * @property offset The offset in bytes from the start of the push constant block.
 * @property size The size in bytes of the push constant range.
 */
data class PushConstantRange(val stage: Int, val offset: Int, val size: Int)

/**
 * Base class for all Vulkan pipelines.
 * @property device The Vulkan device used to create the pipeline.
 * @property layoutHandle The handle to the pipeline layout.
 * @property handle The handle to the Vulkan pipeline object.
 */
sealed class Pipeline(
    protected val device: VkDevice,
    val layoutHandle: Long,
    override val handle: Long
) : Handle<Long>() {

    override fun destroy() {
        vkDestroyPipelineLayout(device, layoutHandle, null)
        vkDestroyPipeline(device, handle, null)
    }

    companion object {
        fun createPipelineLayout(device: VkDevice, stack: MemoryStack, pushConstantRanges: List<PushConstantRange>): Long {
            val layoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack).`sType$Default`()

            if (pushConstantRanges.isNotEmpty()) {
                val pushConstants = VkPushConstantRange.calloc(pushConstantRanges.size, stack)
                pushConstantRanges.forEachIndexed { i, range ->
                    pushConstants[i].stageFlags(range.stage).offset(range.offset).size(range.size)
                }
                layoutCreateInfo.pPushConstantRanges(pushConstants)
            }

            return stack.createHandle({ "Failed to create pipeline layout" }) {
                vkCreatePipelineLayout(device, layoutCreateInfo, null, it)
            }
        }
    }
}
