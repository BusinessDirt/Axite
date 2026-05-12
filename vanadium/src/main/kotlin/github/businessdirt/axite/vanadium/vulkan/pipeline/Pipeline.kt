package github.businessdirt.axite.vanadium.vulkan.pipeline

import github.businessdirt.axite.vanadium.assets.metadata.LayoutBinding
import github.businessdirt.axite.vanadium.assets.metadata.PushConstantRange
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
 * Base class for all Vulkan pipelines.
 * @property device The Vulkan device used to create the pipeline.
 * @property layoutHandle The handle to the pipeline layout.
 * @property handle The handle to the Vulkan pipeline object.
 */
sealed class Pipeline(
    protected val device: VkDevice,
) : Handle<Long>() {

    abstract val layout: PipelineLayout

    override fun destroy() {
        layout.close()
        vkDestroyPipeline(device, handle, null)
    }

    companion object {
        fun mergePushConstants(ranges: List<PushConstantRange>): List<PushConstantRange> {
            return ranges.groupBy { it.offset to it.size }
                .map { (key, group) ->
                    PushConstantRange(
                        stageFlags = group.fold(0) { acc, r -> acc or r.stageFlags },
                        offset = key.first,
                        size = key.second
                    )
                }
        }

        fun mergeLayoutBindings(bindings: List<LayoutBinding>): List<LayoutBinding> {
            return bindings.groupBy { it.binding }
                .map { (binding, group) ->
                    val first = group.first()
                    LayoutBinding(
                        binding = binding,
                        descriptorType = first.descriptorType,
                        descriptorCount = first.descriptorCount,
                        stageFlags = group.fold(0) { acc, b -> acc or b.stageFlags },
                        name = first.name
                    )
                }
        }
    }
}
