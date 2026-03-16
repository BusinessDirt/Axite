package github.businessdirt.axite.vanadium.platform.vulkan

import github.businessdirt.axite.vanadium.utils.memoryStack
import github.businessdirt.axite.vanadium.utils.vkCheck
import org.lwjgl.vulkan.EXTDebugUtils.*
import org.lwjgl.vulkan.VK10.VK_FALSE
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXT
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT
import org.lwjgl.vulkan.VkInstance

class DebugMessenger(private val vkInstance: VkInstance) : VulkanHandle<Long>() {

    private val debugCallback = VkDebugUtilsMessengerCallbackEXT.create { messageSeverity, _, pCallbackData, _ ->
        val message = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData).pMessageString()

        when {
            (messageSeverity and VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0 -> logger.error("VK_DEBUG: {}", message)
            (messageSeverity and VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0 -> logger.warn("VK_DEBUG: {}", message)
            (messageSeverity and VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) != 0 -> logger.info("VK_DEBUG: {}", message)
            else -> logger.debug("VK_DEBUG: {}", message)
        }
        VK_FALSE
    }

    override val handle: Long = memoryStack { stack ->
        val debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
            .`sType$Default`()
            .messageSeverity(
            VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT or
                    VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
            )
            .messageType(
                VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT or
                        VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT or
                        VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
            )
            .pfnUserCallback(debugCallback)

        val pDebugHandle = stack.mallocLong(1)

        vkCheck(vkCreateDebugUtilsMessengerEXT(vkInstance, debugCreateInfo, null, pDebugHandle)) {
            "Failed to create Vulkan Debug Messenger"
        }

        pDebugHandle[0]
    }

    override fun destroy() {
        vkDestroyDebugUtilsMessengerEXT(vkInstance, handle, null)
        debugCallback.free()
    }
}