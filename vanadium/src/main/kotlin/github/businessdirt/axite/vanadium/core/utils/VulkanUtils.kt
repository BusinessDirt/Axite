package github.businessdirt.axite.vanadium.core.utils

import github.businessdirt.axite.vanadium.core.utils.VulkanUtils.memoryTypeFromProperties
import github.businessdirt.axite.vanadium.vulkan.device.PhysicalDevice
import org.apache.logging.log4j.LogManager
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*
import java.lang.reflect.Modifier


object VulkanErrorMapper {
    private val errorMap: Map<Int, String> by lazy {
        VK13::class.java.fields
            .filter { Modifier.isStatic(it.modifiers) && it.type == Int::class.javaPrimitiveType }
            .filter { it.name.startsWith("VK_ERROR_", "VK_NOT_", "VK_TIMEOUT", "VK_EVENT_", "VK_INCOMPLETE") }
            .associate { it.getInt(null) to it.name }
    }

    fun getName(errorCode: Int): String = errorMap[errorCode] ?: "UNKNOWN_VK_ERROR"
}

inline fun vkCheck(
    result: Int,
    lazyErrMsg: () -> String = { "Vulkan operation failed" },
) {
    if (result != VK_SUCCESS) {
        val errorName = VulkanErrorMapper.getName(result)
        throw RuntimeException("$result: $errorName [${lazyErrMsg()}]")
    }
}

object VulkanUtils {
    private val logger = LogManager.getLogger(VulkanUtils::class.java)

    const val PORTABILITY_EXTENSION: String = "VK_KHR_portability_enumeration"
    const val VALIDATION_LAYER: String = "VK_LAYER_KHRONOS_validation"

    val supportedValidationLayers: List<String>
        get() = memoryStack { stack ->
            val pPropertyCount = stack.mallocInt(1)
            vkEnumerateInstanceLayerProperties(pPropertyCount, null)

            val count = pPropertyCount[0]
            val propsBuf = VkLayerProperties.malloc(count, stack)
            vkEnumerateInstanceLayerProperties(pPropertyCount, propsBuf)

            // Collect all supported names
            val supportedNames = (0 until count).map { propsBuf[it].layerNameString() }.distinct()
            logger.debugGrid("Available Validation Layers [${supportedNames.size} total]", supportedNames) { name ->
                val parts = name.split("_")
                if (parts.size >= 3) parts[2] else "CORE"
            }

            return@memoryStack when (VALIDATION_LAYER) {
                in supportedNames -> listOf(VALIDATION_LAYER)
                else -> {
                    logger.warn("Requested layer '{}' not found!", VALIDATION_LAYER)
                    emptyList()
                }
            }
        }

    val instanceExtensions: Set<String>
        get() = memoryStack { stack ->
            val pPropertyCount = stack.mallocInt(1)
            vkEnumerateInstanceExtensionProperties(null as CharSequence?, pPropertyCount, null)

            val count = pPropertyCount[0]
            val propsBuf = VkExtensionProperties.malloc(count, stack)
            vkEnumerateInstanceExtensionProperties(null as CharSequence?, pPropertyCount, propsBuf)

            val names = (0 until count).map { propsBuf[it].extensionNameString() }.toSet()

            logger.debugGrid("Vulkan Instance Extensions [${names.size} total]", names) {
                it.split("_").getOrNull(1) ?: "OTHER"
            }

            return@memoryStack names
        }

    fun VkSurfaceCapabilitiesKHR.coerceRequestedImageCount(requestedImages: Int): Int {
        val min = minImageCount()
        val max = if (maxImageCount() > 0) maxImageCount() else Int.MAX_VALUE

        // If user didn't specify, try triple buffering (3) or min + 1
        val target = if (requestedImages <= 0) 3 else requestedImages
        val result = target.coerceIn(min, max)

        logger.debug(
            "Surface Image Count: [min: {}, max: {}]. Requested: {}, Coerced: {}",
            min,
            if (maxImageCount() == 0) "unlimited" else max,
            target,
            result
        )
        return result
    }

    fun PhysicalDevice.memoryTypeFromProperties(typeBits: Int, reqsMask: Int): Int =
        (0..<VK_MAX_MEMORY_TYPES).firstOrNull { i ->
            val isTypeSupported = (typeBits and (1 shl i)) != 0
            val hasRequiredProperties = (memoryProperties.memoryTypes().get(i).propertyFlags() and reqsMask) == reqsMask
            isTypeSupported && hasRequiredProperties
        } ?: throw RuntimeException("Failed to find suitable memory type (typeBits: $typeBits, reqsMask: $reqsMask)")
}

fun VkMemoryAllocateInfo.findMemoryTypeIndex(physicalDevice: PhysicalDevice, typeBits: Int, reqsMask: Int): VkMemoryAllocateInfo = apply {
    memoryTypeIndex(physicalDevice.memoryTypeFromProperties(typeBits, reqsMask))
}

/**
 * Decodes a 32-bit Vulkan version integer into a human-readable string.
 */
fun Int.decodeVersion(): String {
    val major = (this shr 22) and 0x7F
    val minor = (this shr 12) and 0x3FF
    val patch = this and 0xFFF
    return "$major.$minor.$patch"
}

/**
 * Maps the VkPhysicalDeviceType integer to its readable enum name.
 */
fun Int.decodeDeviceType(): String = when (this) {
    0 -> "OTHER"
    1 -> "INTEGRATED_GPU"
    2 -> "DISCRETE_GPU"
    3 -> "VIRTUAL_GPU"
    4 -> "CPU"
    else -> "UNKNOWN_TYPE ($this)"
}

/**
 * Maps the VkFormat integer to its readable name.
 */
fun Int.decodeFormat(): String = when (this) {
    0 -> "UNDEFINED"
    37 -> "R8G8B8A8_UNORM"
    43 -> "R8G8B8A8_SRGB"
    44 -> "B8G8R8A8_UNORM"
    50 -> "B8G8R8A8_SRGB"
    58 -> "R16G16B16A16_SFLOAT"
    64 -> "R32G32B32A16_SFLOAT"
    97 -> "R8G8B8A8_SNORM"
    1000156000 -> "A2B10G10R10_UNORM_PACK32"
    else -> "FORMAT_$this"
}

/**
 * Maps the VkColorSpaceKHR integer to its readable name.
 */
fun Int.decodeColorSpace(): String = when (this) {
    0 -> "SRGB_NONLINEAR"
    1000104001 -> "DISPLAY_P3_NONLINEAR"
    1000104002 -> "EXTENDED_SRGB_LINEAR"
    1000104010 -> "ADOBERGB_NONLINEAR"
    1000104014 -> "BT2020_LINEAR"
    else -> "CS_$this"
}

fun Long.runIfNonNull(block: Long.() -> Unit): Long {
    if (this != MemoryUtil.NULL) this.run(block)
    return this
}

fun MemoryStack.imageBarrier(
    cmdHandle: VkCommandBuffer,
    image: Long,
    oldLayout: Int,
    newLayout: Int,
    srcStage: Long,
    dstStage: Long,
    srcAccess: Long,
    dstAccess: Long,
    aspectMask: Int
) {
    val imageBarrier = VkImageMemoryBarrier2.calloc(1, this).`sType$Default`()
        .oldLayout(oldLayout)
        .newLayout(newLayout)
        .srcStageMask(srcStage)
        .dstStageMask(dstStage)
        .srcAccessMask(srcAccess)
        .dstAccessMask(dstAccess)
        .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        .image(image)
        .subresourceRange { it
            .aspectMask(aspectMask)
            .baseMipLevel(0)
            .levelCount(VK_REMAINING_MIP_LEVELS)
            .baseArrayLayer(0)
            .layerCount(VK_REMAINING_ARRAY_LAYERS)
        }

    val dependencyInfo = VkDependencyInfo.calloc(this).`sType$Default`().pImageMemoryBarriers(imageBarrier)
    vkCmdPipelineBarrier2(cmdHandle, dependencyInfo)
}