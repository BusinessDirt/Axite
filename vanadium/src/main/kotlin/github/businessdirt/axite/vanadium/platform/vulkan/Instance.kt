package github.businessdirt.axite.vanadium.platform.vulkan


import github.businessdirt.axite.vanadium.VanadiumConfig
import github.businessdirt.axite.vanadium.utils.*
import github.businessdirt.axite.vanadium.utils.VulkanUtils.PORTABILITY_EXTENSION
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkApplicationInfo
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkInstanceCreateInfo

class Instance(config: VanadiumConfig) : VulkanHandle<VkInstance>() {

    override val handle: VkInstance = memoryStack { stack ->
        val appShortName = stack.UTF8(config.applicationName)
        val appInfo = VkApplicationInfo.calloc(stack)
            .`sType$Default`()
            .pApplicationName(appShortName)
            .applicationVersion(1)
            .pEngineName(appShortName)
            .engineVersion(0)
            .apiVersion(VK_API_VERSION_1_3)

        val validationLayers = VulkanUtils.supportedValidationLayers
        val supportsValidation = config.validate && validationLayers.isNotEmpty()

        if (config.validate && validationLayers.isEmpty()) {
            logger.warn("Request validation but no supported validation layers found. Falling back to no validation")
        }

        val requiredLayers = if (supportsValidation) {
            stack.mallocPointer(validationLayers.size).apply {
                validationLayers.forEach { put(stack.ASCII(it)) }
                flip()
            }
        } else null

        val usePortability = PORTABILITY_EXTENSION in VulkanUtils.instanceExtensions &&
                PlatformUtils.type == Platform.MACOS

        // GLFW Extensions
        val glfwExtensions: PointerBuffer = GLFWVulkan.glfwGetRequiredInstanceExtensions()
            ?: error("Failed to find the GLFW platform surface extensions")

        val customExtensions = mutableListOf<String>().apply {
            if (supportsValidation) add(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)
            if (usePortability) add(PORTABILITY_EXTENSION)
        }

        // Combine GLFW and custom extensions into one PointerBuffer
        val requiredExtensions = stack.mallocPointer(glfwExtensions.remaining() + customExtensions.size).apply {
            put(glfwExtensions)
            customExtensions.forEach { put(stack.UTF8(it)) }
            flip()
        }

        // Create instance info
        val instanceInfo = VkInstanceCreateInfo.calloc(stack)
            .`sType$Default`()
            .pApplicationInfo(appInfo)
            .ppEnabledLayerNames(requiredLayers)
            .ppEnabledExtensionNames(requiredExtensions)

        if (usePortability) {
            instanceInfo.flags(0x00000001) // VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR
        }

        val instanceHandle = stack.createPointer({ "Error creating instance" }) { pointerBuffer ->
            vkCreateInstance(instanceInfo, null, pointerBuffer)
        }
        VkInstance(instanceHandle, instanceInfo)
    }

    override fun destroy() =
        vkDestroyInstance(handle, null)
}