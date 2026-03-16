package github.businessdirt.axite.vanadium.utils

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.EXTDebugUtils.*
import org.lwjgl.vulkan.VK10.vkEnumerateInstanceExtensionProperties
import org.lwjgl.vulkan.VK10.vkEnumerateInstanceLayerProperties
import org.lwjgl.vulkan.VK13
import org.lwjgl.vulkan.VK13.VK_SUCCESS
import org.lwjgl.vulkan.VkExtensionProperties
import org.lwjgl.vulkan.VkLayerProperties
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
                logger.debug("Supported layer [{}]", layerName)
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
            logger.debug("Instance supports [{}] extensions", count)

            val propsBuf = VkExtensionProperties.malloc(count, stack)
            vkEnumerateInstanceExtensionProperties(null as CharSequence?, pPropertyCount, propsBuf)

            return@memoryStack (0 until count).map { i ->
                val extensionName = propsBuf[i].extensionNameString()
                logger.debug("Supported instance extension [{}]", extensionName)
                extensionName
            }.toSet()
        }
}