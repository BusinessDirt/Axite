package github.businessdirt.axite.vanadium.platform.vulkan.pipeline

import github.businessdirt.axite.vanadium.platform.vulkan.Context
import github.businessdirt.axite.vanadium.platform.vulkan.Device
import github.businessdirt.axite.vanadium.platform.vulkan.VulkanHandle
import github.businessdirt.axite.vanadium.utils.createHandle
import github.businessdirt.axite.vanadium.utils.memoryStack
import org.lwjgl.vulkan.VK10.vkCreatePipelineCache
import org.lwjgl.vulkan.VK10.vkDestroyPipelineCache
import org.lwjgl.vulkan.VkPipelineCacheCreateInfo

class PipelineCache(device: Device) : VulkanHandle<Long>(){
    override val handle: Long = memoryStack { stack ->
        val createInfo = VkPipelineCacheCreateInfo.calloc(stack).`sType$Default`()
        stack.createHandle({ "Error creating pipeline cache" }) {
            vkCreatePipelineCache(device.handle, createInfo, null, it)
        }
    }

    override fun destroy() = vkDestroyPipelineCache(Context.device.handle, handle, null)
}