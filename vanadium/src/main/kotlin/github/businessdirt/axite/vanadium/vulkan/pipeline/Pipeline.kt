package github.businessdirt.axite.vanadium.vulkan.pipeline

import github.businessdirt.axite.vanadium.assets.metadata.LayoutBinding
import github.businessdirt.axite.vanadium.assets.metadata.PushConstantRange
import github.businessdirt.axite.vanadium.assets.types.Shader
import github.businessdirt.axite.vanadium.vulkan.Handle
import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.spvc.Spvc.*
import org.lwjgl.vulkan.VK10.vkDestroyPipeline
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkSpecializationInfo
import org.lwjgl.vulkan.VkSpecializationMapEntry
import java.nio.ByteBuffer

sealed class Pipeline(
    protected val device: VkDevice,
) : Handle<Long>() {

    abstract val layout: PipelineLayout

    override fun destroy() {
        layout.close()
        vkDestroyPipeline(device, handle, null)
    }

    abstract fun bind(commandBuffer: CommandBuffer)

    protected fun MemoryStack.specializationInfo(shader: Shader, values: Map<String, Any>): VkSpecializationInfo? {
        val constants = shader.metadata.specializationConstants
        val relevantConstants = constants.filter { it.name in values }
        if (relevantConstants.isEmpty()) return null

        val mapEntries = VkSpecializationMapEntry.calloc(relevantConstants.size, this)
        var totalSize = 0
        for (const in relevantConstants) {
            totalSize += getBasetypeSize(const.type)
        }

        val pData = this.malloc(totalSize)
        var offset = 0
        relevantConstants.forEachIndexed { i, const ->
            val size = getBasetypeSize(const.type)
            mapEntries[i].constantID(const.constantId).offset(offset).size(size.toLong())

            val value = values[const.name]!!
            putValue(pData, offset, value, const.type)

            offset += size
        }

        return VkSpecializationInfo.calloc(this)
            .pMapEntries(mapEntries)
            .pData(pData)
    }

    private fun getBasetypeSize(type: Int): Int = when (type) {
        SPVC_BASETYPE_BOOLEAN, SPVC_BASETYPE_INT32, SPVC_BASETYPE_UINT32, SPVC_BASETYPE_FP32 -> 4
        SPVC_BASETYPE_INT64, SPVC_BASETYPE_UINT64, SPVC_BASETYPE_FP64 -> 8
        else -> 0
    }

    private fun putValue(data: ByteBuffer, offset: Int, value: Any, type: Int) {
        when (type) {
            SPVC_BASETYPE_BOOLEAN -> data.putInt(offset, if (value as Boolean) 1 else 0)
            SPVC_BASETYPE_INT32 -> data.putInt(offset, value as Int)
            SPVC_BASETYPE_UINT32 -> data.putInt(offset, (value as Number).toInt())
            SPVC_BASETYPE_FP32 -> data.putFloat(offset, (value as Number).toFloat())
            SPVC_BASETYPE_INT64 -> data.putLong(offset, (value as Number).toLong())
            SPVC_BASETYPE_UINT64 -> data.putLong(offset, (value as Number).toLong())
            SPVC_BASETYPE_FP64 -> data.putDouble(offset, (value as Number).toDouble())
            else -> {}
        }
    }

    companion object {
        fun mergePushConstants(ranges: List<PushConstantRange>): List<PushConstantRange> = ranges.groupBy { it.offset to it.size }
            .map { (key, group) ->
                PushConstantRange(
                    stageFlags = group.fold(0) { acc, r -> acc or r.stageFlags },
                    offset = key.first,
                    size = key.second
                )
            }

        fun mergeLayoutBindings(bindings: List<LayoutBinding>): List<LayoutBinding> = bindings.groupBy { it.set to it.binding }
            .map { (key, group) ->
                val first = group.first()
                LayoutBinding(
                    set = key.first,
                    binding = key.second,
                    descriptorType = first.descriptorType,
                    descriptorCount = first.descriptorCount,
                    stageFlags = group.fold(0) { acc, b -> acc or b.stageFlags },
                    name = first.name
                )
            }
    }
}
