package github.businessdirt.axite.vanadium.platform.vulkan

import github.businessdirt.axite.vanadium.platform.vulkan.PhysicalDevice.Companion.REQUIRED_EXTENSIONS
import github.businessdirt.axite.vanadium.utils.debugTree
import github.businessdirt.axite.vanadium.utils.decodeDeviceType
import github.businessdirt.axite.vanadium.utils.decodeVersion
import github.businessdirt.axite.vanadium.utils.memoryStack
import github.businessdirt.axite.vanadium.utils.vkCheck
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU
import org.lwjgl.vulkan.VK13.VK_QUEUE_GRAPHICS_BIT
import org.lwjgl.vulkan.VK13.vkEnumerateDeviceExtensionProperties
import org.lwjgl.vulkan.VK13.vkGetPhysicalDeviceFeatures
import org.lwjgl.vulkan.VK13.vkGetPhysicalDeviceMemoryProperties
import org.lwjgl.vulkan.VK13.vkGetPhysicalDeviceQueueFamilyProperties
import org.lwjgl.vulkan.VK13.vkGetPhysicalDeviceProperties2
import org.lwjgl.vulkan.VK13.vkEnumeratePhysicalDevices


class PhysicalDevice(
    override val handle: VkPhysicalDevice
) : VulkanHandle<VkPhysicalDevice>() {

    val properties: VkPhysicalDeviceProperties2 = VkPhysicalDeviceProperties2.calloc().`sType$Default`().also {
        vkGetPhysicalDeviceProperties2(handle, it)
    }

    val features: VkPhysicalDeviceFeatures = VkPhysicalDeviceFeatures.calloc().also {
        vkGetPhysicalDeviceFeatures(handle, it)
    }

    val memoryProperties: VkPhysicalDeviceMemoryProperties = VkPhysicalDeviceMemoryProperties.calloc().also {
        vkGetPhysicalDeviceMemoryProperties(handle, it)
    }

    val deviceName: String = properties.properties().deviceNameString()

    val extensions: VkExtensionProperties.Buffer = memoryStack { stack ->
        val pCount = stack.mallocInt(1)
        vkCheck(vkEnumerateDeviceExtensionProperties(handle, null as CharSequence?, pCount, null)) {
            "Failed to get number of device extension properties"
        }

        // The last line of the lambda is what gets assigned to 'vkDeviceExtensions'
        VkExtensionProperties.calloc(pCount[0]).also {
            vkCheck(vkEnumerateDeviceExtensionProperties(handle, null as CharSequence?, pCount, it)) {
                "Failed to get extension properties"
            }
        }
    }

    val queueFamilyProperties: VkQueueFamilyProperties.Buffer = memoryStack { stack ->
        val pCount = stack.mallocInt(1)
        vkGetPhysicalDeviceQueueFamilyProperties(handle, pCount, null)

        VkQueueFamilyProperties.calloc(pCount[0]).also {
            vkGetPhysicalDeviceQueueFamilyProperties(handle, pCount, it)
        }
    }

    val hasGraphicsQueueFamily: Boolean
        get() = (0 until queueFamilyProperties.capacity()).any { i ->
            (queueFamilyProperties[i].queueFlags() and VK_QUEUE_GRAPHICS_BIT) != 0
        }

    fun supportsExtensions(requiredExtensions: Set<String>): Boolean {
        val supportedExtensions = (0 until extensions.capacity()).map { i ->
            extensions[i].extensionNameString()
        }.toSet()

        val missingExtensions = requiredExtensions - supportedExtensions

        if (missingExtensions.isNotEmpty()) {
            logger.debug("Extension [${missingExtensions.first()}] is not supported by device [$deviceName]")
            return false
        }

        return true
    }

    override fun destroy() {
        memoryProperties.free()
        features.free()
        queueFamilyProperties.free()
        extensions.free()
        properties.free()
    }

    companion object {
        val REQUIRED_EXTENSIONS: Set<String> = setOf(
            KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME
        )
    }
}

fun Instance.pickPhysicalDevice(): PhysicalDevice = memoryStack { stack ->
    val pPhysicalDevices: PointerBuffer = this.getPhysicalDevices(stack)
    val allDevices = (0 until pPhysicalDevices.capacity()).map { i ->
        // We pass 'this.vkInstance' directly
        PhysicalDevice(VkPhysicalDevice(pPhysicalDevices[i], this@pickPhysicalDevice.handle))
    }

    val (validDevices, invalidDevices) = allDevices.partition { device ->
        val hasGraphics = device.hasGraphicsQueueFamily
        val hasExtensions = device.supportsExtensions(REQUIRED_EXTENSIONS)

        if (!hasGraphics) logger.debug("Device [{}] missing graphics queue", device.deviceName)
        if (!hasExtensions) logger.debug("Device [{}] missing required extensions", device.deviceName)

        hasGraphics && hasExtensions
    }

    invalidDevices.forEach { it.cleanup() }

    check(validDevices.isNotEmpty()) { "No suitable physical devices found" }

    val sortedDevices = validDevices.sortedByDescending { device ->
        when {
            device.properties.properties().deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU -> 50
            else -> 10
        }
    }

    val winner: PhysicalDevice = sortedDevices.first()
    logger.debug("Selected device: {} ({})", winner.deviceName, winner.properties.properties().deviceType().decodeDeviceType())
    logger.debug("├── API Version: {}", winner.properties.properties().apiVersion().decodeVersion())
    logger.debug("├── Driver Version: {}", winner.properties.properties().driverVersion().decodeVersion())
    logger.debug("├── Device ID: {}", winner.properties.properties().deviceID())
    winner.properties.properties().limits().debugTree("├── Limits") {
        val prefix = if (it.contains("Limits")) "" else "│   "
        logger.debug("$prefix$it")
    }
    winner.features.debugTree("└── Features") {
        val prefix = if (it.contains("Features")) "" else "    "
        logger.debug("$prefix$it")
    }

    sortedDevices.drop(1).forEach { it.cleanup() }

    return@memoryStack winner
}

private fun Instance.getPhysicalDevices(stack: MemoryStack): PointerBuffer {
    val pDeviceCount = stack.mallocInt(1)

    vkCheck(vkEnumeratePhysicalDevices(this.handle, pDeviceCount, null)) {
        "Failed to get number of physical devices"
    }

    val deviceCount = pDeviceCount[0]
    logger.debug("Detected {} physical device{}", deviceCount, if (deviceCount > 1) "s" else "")

    val pPhysicalDevices = stack.mallocPointer(deviceCount)
    vkCheck(vkEnumeratePhysicalDevices(this.handle, pDeviceCount, pPhysicalDevices)) {
        "Failed to get physical devices"
    }

    return pPhysicalDevices
}