package github.businessdirt.axite.vanadium.vulkan.pipeline

import github.businessdirt.axite.vanadium.assets.metadata.LayoutBinding
import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.Handle
import org.lwjgl.vulkan.VK13.vkCreateDescriptorSetLayout
import org.lwjgl.vulkan.VK13.vkDestroyDescriptorSetLayout
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo
import org.lwjgl.vulkan.VkDevice

class DescriptorSetLayout(
    val device: VkDevice,
    bindings: List<LayoutBinding>
) : Handle<Long>() {

    override val handle: Long = memoryStack { stack ->
        val vkBindings = VkDescriptorSetLayoutBinding.calloc(bindings.size, stack)
        bindings.forEachIndexed { i, binding ->
            vkBindings[i]
                .binding(binding.binding)
                .descriptorType(binding.descriptorType)
                .descriptorCount(binding.descriptorCount)
                .stageFlags(binding.stageFlags)
        }

        val createInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack).`sType$Default`()
            .pBindings(vkBindings)

        stack.createHandle({ "Failed to create descriptor set layout" }) {
            vkCreateDescriptorSetLayout(device, createInfo, null, it)
        }
    }

    override fun destroy() {
        vkDestroyDescriptorSetLayout(device, handle, null)
    }
}
