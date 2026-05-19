package github.businessdirt.axite.vanadium.vulkan.pipeline

import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.core.utils.vkCheck
import github.businessdirt.axite.vanadium.vulkan.Handle
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkDescriptorImageInfo
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkWriteDescriptorSet

class DescriptorSet(
    val device: VkDevice,
    val pool: DescriptorPool,
    val layout: DescriptorSetLayout
) : Handle<Long>() {

    override val handle: Long = memoryStack { stack ->
        val layouts = stack.longs(layout.handle)
        val allocateInfo = VkDescriptorSetAllocateInfo.calloc(stack).`sType$Default`()
            .descriptorPool(pool.handle)
            .pSetLayouts(layouts)

        val pDescriptorSet = stack.mallocLong(1)
        vkCheck(vkAllocateDescriptorSets(device, allocateInfo, pDescriptorSet)) {
            "Failed to allocate descriptor set"
        }
        pDescriptorSet[0]
    }

    fun updateImage(binding: Int, view: Long, sampler: Long, layout: Int = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, type: Int = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER) = memoryStack { stack ->
        val imageInfo = VkDescriptorImageInfo.calloc(1, stack)
            .imageView(view)
            .sampler(sampler)
            .imageLayout(layout)

        val write = VkWriteDescriptorSet.calloc(1, stack).`sType$Default`()
            .dstSet(handle)
            .dstBinding(binding)
            .dstArrayElement(0)
            .descriptorType(type)
            .descriptorCount(1)
            .pImageInfo(imageInfo)

        vkUpdateDescriptorSets(device, write, null)
    }

    override fun destroy() {
        // Descriptor sets are freed when the pool is destroyed, 
        // but we used VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT so we can free it manually if needed.
        memoryStack { stack ->
            val pDescriptorSet = stack.longs(handle)
            vkFreeDescriptorSets(device, pool.handle, pDescriptorSet)
        }
    }
}
