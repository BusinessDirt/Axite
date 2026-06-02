package github.businessdirt.axite.vanadium.vulkan.descriptors

import github.businessdirt.axite.vanadium.assets.metadata.LayoutBinding
import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.Handle
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*

class DescriptorSetLayout(
    val device: VkDevice,
    bindings: List<LayoutBinding>
) : Handle<Long>() {

    override val handle: Long = memoryStack { stack ->
        val vkBindings = VkDescriptorSetLayoutBinding.calloc(bindings.size, stack)
        val bindingFlags = stack.mallocInt(bindings.size)
        var layoutFlags = 0

        bindings.forEachIndexed { i, binding ->
            vkBindings[i]
                .binding(binding.binding)
                .descriptorType(binding.descriptorType)
                .descriptorCount(binding.descriptorCount)
                .stageFlags(binding.stageFlags)

            // If we have an array, assume we want bindless features
            if (binding.descriptorCount > 1) {
                bindingFlags.put(i, VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT or VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT)
                layoutFlags = layoutFlags or VK_DESCRIPTOR_SET_LAYOUT_CREATE_UPDATE_AFTER_BIND_POOL_BIT
            } else {
                bindingFlags.put(i, 0)
            }
        }

        val flagsCreateInfo = VkDescriptorSetLayoutBindingFlagsCreateInfo.calloc(stack).`sType$Default`()
            .pBindingFlags(bindingFlags)

        val createInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).`sType$Default`()
            .pNext(flagsCreateInfo.address())
            .pBindings(vkBindings)
            .flags(layoutFlags)

        stack.createHandle({ "Failed to create descriptor set layout" }) {
            vkCreateDescriptorSetLayout(device, createInfo, null, it)
        }
    }

    override fun destroy() {
        vkDestroyDescriptorSetLayout(device, handle, null)
    }
}
