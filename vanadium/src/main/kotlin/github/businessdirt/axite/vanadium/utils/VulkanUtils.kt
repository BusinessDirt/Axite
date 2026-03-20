package github.businessdirt.axite.vanadium.utils

import github.businessdirt.axite.vanadium.platform.vulkan.Context
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK13
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkDependencyInfo
import org.lwjgl.vulkan.VkExtensionProperties
import org.lwjgl.vulkan.VkImageMemoryBarrier2
import org.lwjgl.vulkan.VkLayerProperties
import org.lwjgl.vulkan.VkMemoryAllocateInfo
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(VulkanUtils::class.java)

    const val PORTABILITY_EXTENSION: String = "VK_KHR_portability_enumeration"
    const val VALIDATION_LAYER: String = "VK_LAYER_KHRONOS_validation"
    const val MAX_FRAMES_IN_FLIGHT: Int = 2

    val supportedValidationLayers: List<String>
        get() = memoryStack { stack ->
            val pPropertyCount = stack.mallocInt(1)
            vkEnumerateInstanceLayerProperties(pPropertyCount, null)

            val count = pPropertyCount[0]
            logger.debug("Instance supports [{}] layers", count)

            val propsBuf = VkLayerProperties.malloc(count, stack)
            vkEnumerateInstanceLayerProperties(pPropertyCount, propsBuf)

            val supportedLayers = (0 until count).map { i ->
                val layerName = propsBuf[i].layerNameString()
                logger.debug("{}{}", if (i == count - 1) "└── " else "├── ", layerName)
                layerName
            }

            return@memoryStack when (VALIDATION_LAYER) {
                in supportedLayers -> listOf(VALIDATION_LAYER)
                else -> emptyList()
            }
        }

    val instanceExtensions: Set<String>
        get() = memoryStack { stack ->
            val pPropertyCount = stack.mallocInt(1)
            vkEnumerateInstanceExtensionProperties(null as CharSequence?, pPropertyCount, null)

            val count = pPropertyCount[0]
            logger.debug("Instance supports [{}] extensions:", count)

            val propsBuf = VkExtensionProperties.malloc(count, stack)
            vkEnumerateInstanceExtensionProperties(null as CharSequence?, pPropertyCount, propsBuf)

            return@memoryStack (0 until count).map { i ->
                val extensionName = propsBuf[i].extensionNameString()
                logger.debug("{}{}", if (i == count - 1) "└── " else "├── ", extensionName)
                extensionName
            }.toSet()
        }

    fun memoryTypeFromProperties(typeBits: Int, reqsMask: Int): Int {
        val memoryTypes = Context.physicalDevice.memoryProperties.memoryTypes()

        return (0..<VK_MAX_MEMORY_TYPES).firstOrNull { i ->
            val isTypeSupported = (typeBits and (1 shl i)) != 0
            val hasRequiredProperties = (memoryTypes.get(i).propertyFlags() and reqsMask) == reqsMask
            isTypeSupported && hasRequiredProperties
        } ?: throw RuntimeException("Failed to find suitable memory type (typeBits: $typeBits, reqsMask: $reqsMask)")
    }
}

fun VkMemoryAllocateInfo.findMemoryTypeIndex(typeBits: Int, reqsMask: Int): VkMemoryAllocateInfo = apply {
    memoryTypeIndex(VulkanUtils.memoryTypeFromProperties(typeBits, reqsMask))
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