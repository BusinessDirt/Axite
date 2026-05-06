package github.businessdirt.axite.vanadium.vulkan.device

import github.businessdirt.axite.vanadium.core.utils.*
import github.businessdirt.axite.vanadium.vulkan.Handle
import github.businessdirt.axite.vanadium.vulkan.Instance
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*
import kotlin.math.max

class PhysicalDevice(
    override val handle: VkPhysicalDevice
) : Handle<VkPhysicalDevice>() {

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

    val hasComputeQueueFamily: Boolean
        get() = (0 until queueFamilyProperties.capacity()).any { i ->
            (queueFamilyProperties[i].queueFlags() and VK_QUEUE_COMPUTE_BIT) != 0
        }

    val hasPresentationSupport: Boolean
        get() = (0 until queueFamilyProperties.capacity()).any { i ->
            GLFWVulkan.glfwGetPhysicalDevicePresentationSupport(handle.instance, handle, i)
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
}

fun Instance.pickPhysicalDevice(): PhysicalDevice = memoryStack { stack ->
    val pPhysicalDevices = this.getPhysicalDevices(stack)
    val allDevices = (0 until pPhysicalDevices.capacity()).map { i ->
        PhysicalDevice(VkPhysicalDevice(pPhysicalDevices[i], this@pickPhysicalDevice.handle))
    }

    val requirements: List<PhysicalDeviceSelector.Requirement> = PhysicalDeviceSelector.getRequirements()
    val (validDevices, invalidDevices) = allDevices.partition { device ->
        requirements.filter { it.mandatory }.all { req ->
            when (val result = req.check(device)) {
                is Boolean -> result
                is Number -> result.toInt() > 0
                else -> true
            }
        }
    }

    // Log rejected devices
    if (invalidDevices.isNotEmpty()) {
        val rejectionReasons = invalidDevices.associate { device ->
            val failedMandatory = requirements.filter { it.mandatory }.filterNot { req ->
                when (val result = req.check(device)) {
                    is Boolean -> result
                    is Number -> result.toInt() > 0
                    else -> true
                }
            }
            device.deviceName to failedMandatory.joinToString(", ") {
                it.message.ifBlank { it.name }
            }
        }

        LogManager.getLogger(PhysicalDevice::class.java)
            .debugGrid("Rejected Physical Devices", rejectionReasons.keys) { deviceName ->
                rejectionReasons[deviceName] ?: "UNKNOWN"
            }

        invalidDevices.forEach { it.close() }
    }

    check(validDevices.isNotEmpty()) { "Failed to find a GPU with Vulkan support!" }

    // Scoring logic
    val winner = validDevices.maxByOrNull { device ->
        requirements.sumOf { req ->
            when (val result = req.check(device)) {
                is Boolean -> if (result) req.weight else 0
                is Number -> result.toInt() * max(1, req.weight)
                else -> 0
            }
        }
    }!!

    // Query the extensions for the selected winner
    val deviceExtensions = memoryStack { stack ->
        val pPropertyCount = stack.mallocInt(1)
        vkEnumerateDeviceExtensionProperties(winner.handle, null as CharSequence?, pPropertyCount, null)

        val props = VkExtensionProperties.malloc(pPropertyCount[0], stack)
        vkEnumerateDeviceExtensionProperties(winner.handle, null as CharSequence?, pPropertyCount, props)

        (0 until pPropertyCount[0]).map { props[it].extensionNameString() }.toSet()
    }

    val props = winner.properties.properties()
    val vramBytes = (0 until winner.memoryProperties.memoryHeapCount())
        .map { winner.memoryProperties.memoryHeaps(it) }
        .filter { (it.flags() and VK_MEMORY_HEAP_DEVICE_LOCAL_BIT) != 0 }
        .sumOf { it.size() }

    boxedString(boxCharset = BoxCharset.ROUNDED, title = "Selected Physical Device: [ ${winner.deviceName} ]") {
        appendLine("Type: ${props.deviceType().decodeDeviceType()}")
        appendLine("Driver: ${props.driverVersion().decodeVersion()}")
        appendLine("API: ${props.apiVersion().decodeVersion()}")
        appendLine("VRAM: ${vramBytes / (1024 * 1024)} MB")
        appendLine("ID: 0x${Integer.toHexString(props.deviceID())}")
    }.log(LogManager.getLogger(PhysicalDevice::class.java), Level.DEBUG)

    // Log extensions for this specific device
    LogManager.getLogger(PhysicalDevice::class.java)
        .debugGrid("Device Extensions Supported", deviceExtensions) {
            it.split("_").getOrNull(1) ?: "OTHER"
        }

    // Cleanup non-winners
    validDevices.filter { it != winner }.forEach { it.close() }

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