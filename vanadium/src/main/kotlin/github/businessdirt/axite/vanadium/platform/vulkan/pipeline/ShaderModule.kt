package github.businessdirt.axite.vanadium.platform.vulkan.pipeline

import github.businessdirt.axite.vanadium.platform.vulkan.Context
import github.businessdirt.axite.vanadium.platform.vulkan.VulkanHandle
import github.businessdirt.axite.vanadium.utils.createHandle
import github.businessdirt.axite.vanadium.utils.memoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10.vkCreateShaderModule
import org.lwjgl.vulkan.VK10.vkDestroyShaderModule
import org.lwjgl.vulkan.VkShaderModuleCreateInfo
import java.io.File

class ShaderModule(
    val shaderStage: Int,
    shaderSpvFile: String
) : VulkanHandle<Long>() {

    override val handle: Long = try {
        val code = File(shaderSpvFile).readBytes()
        val pCode = MemoryUtil.memAlloc(code.size).put(code).flip()

        try {
            memoryStack { stack ->
                val createInfo = VkShaderModuleCreateInfo.calloc(stack).`sType$Default`().pCode(pCode)
                stack.createHandle({ "Failed to create shader module" }) {
                    vkCreateShaderModule(Context.device.handle, createInfo, null, it)
                }
            }
        } finally {
            MemoryUtil.memFree(pCode)
        }

    } catch (e: Exception) {
        throw RuntimeException("Failed to load shader: $shaderSpvFile", e)
    }

    override fun destroy() = vkDestroyShaderModule(Context.device.handle, handle, null)
}