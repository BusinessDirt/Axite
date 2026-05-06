package github.businessdirt.axite.vanadium.vulkan

import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import org.apache.logging.log4j.MarkerManager
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.vkCreateDebugUtilsMessengerEXT
import org.lwjgl.vulkan.EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT
import org.lwjgl.vulkan.VK13.VK_FALSE
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXT
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT
import org.lwjgl.vulkan.VkInstance

class DebugMessenger(private val instance: VkInstance) : Handle<Long>() {

    private val debugCallback = VkDebugUtilsMessengerCallbackEXT.create { messageSeverity, _, pCallbackData, _ ->
        val message = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData).pMessageString()

        val marker = MarkerManager.getMarker("VK_DEBUG")
        val logBuilder = when {
            (messageSeverity and VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0 -> logger.atError()
            (messageSeverity and VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0 -> logger.atWarn()
            (messageSeverity and VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) != 0 -> logger.atInfo()
            else -> logger.atDebug()
        }

        logBuilder.withMarker(marker).log(message)

        VK_FALSE
    }

    override val handle: Long = memoryStack { stack ->
        val debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack).`sType$Default`()
            .messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT or
                VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT)
            .messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT or
                VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT or
                VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)
            .pfnUserCallback(debugCallback)

        stack.createHandle({ "Failed to create Vulkan Debug Messenger" }) { longBuffer ->
            vkCreateDebugUtilsMessengerEXT(instance, debugCreateInfo, null, longBuffer)
        }
    }

    override fun destroy() {
        vkDestroyDebugUtilsMessengerEXT(instance, handle, null)
        debugCallback.free()
    }
}