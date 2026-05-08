package github.businessdirt.axite.vanadium.vulkan.pipeline

import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.Handle
import org.lwjgl.vulkan.VK13.vkCreatePipelineCache
import org.lwjgl.vulkan.VK13.vkDestroyPipelineCache
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPipelineCacheCreateInfo

class PipelineCache(private val device: VkDevice) : Handle<Long>() {
    override val handle: Long = memoryStack { stack ->
        val createInfo = VkPipelineCacheCreateInfo.calloc(stack).`sType$Default`()
        stack.createHandle({ "Error creating pipeline cache" }) {
            vkCreatePipelineCache(device, createInfo, null, it)
        }
    }

    override fun destroy() = vkDestroyPipelineCache(device, handle, null)
}