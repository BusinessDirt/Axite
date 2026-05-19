package github.businessdirt.axite.vanadium.vulkan.pipeline

import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.Handle
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo
import org.lwjgl.vulkan.VkDescriptorPoolSize
import org.lwjgl.vulkan.VkDevice

class DescriptorPool(
    val device: VkDevice,
    maxSets: Int,
    poolSizes: List<PoolSize>
) : Handle<Long>() {

    data class PoolSize(val type: Int, val count: Int)

    override val handle: Long = memoryStack { stack ->
        val sizes = VkDescriptorPoolSize.calloc(poolSizes.size, stack)
        poolSizes.forEachIndexed { i, size ->
            sizes[i].type(size.type).descriptorCount(size.count)
        }

        val createInfo = VkDescriptorPoolCreateInfo.calloc(stack).`sType$Default`()
            .pPoolSizes(sizes)
            .maxSets(maxSets)
            .flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)

        stack.createHandle({ "Failed to create descriptor pool" }) {
            vkCreateDescriptorPool(device, createInfo, null, it)
        }
    }

    override fun destroy() {
        vkDestroyDescriptorPool(device, handle, null)
    }
}
