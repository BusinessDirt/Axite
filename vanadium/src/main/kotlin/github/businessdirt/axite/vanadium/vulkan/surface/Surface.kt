package github.businessdirt.axite.vanadium.vulkan.surface

import github.businessdirt.axite.vanadium.core.utils.*
import github.businessdirt.axite.vanadium.vulkan.Handle
import github.businessdirt.axite.vanadium.vulkan.Instance
import github.businessdirt.axite.vanadium.vulkan.device.PhysicalDevice
import org.apache.logging.log4j.Level
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.vulkan.KHRSurface
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR
import org.lwjgl.vulkan.VkSurfaceFormatKHR
import kotlin.math.max

class Surface(
    physicalDevice: PhysicalDevice,
    private val instance: Instance,
    windowHandle: Long
) : Handle<Long>() {

    data class SurfaceFormat(val imageFormat: Int, val colorSpace: Int)

    override val handle: Long = memoryStack { stack ->
        stack.createHandle({ "Failed to create window surface" }) { longBuffer ->
            GLFWVulkan.glfwCreateWindowSurface(instance.handle, windowHandle, null, longBuffer)
        }
    }

    var surfaceCaps: VkSurfaceCapabilitiesKHR = VkSurfaceCapabilitiesKHR.calloc().also {
        vkCheck(KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice.handle, handle, it)) {
            "Failed to get surface capabilities"
        }
    }

    fun updateCaps(physicalDevice: PhysicalDevice) {
        val result = KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice.handle, handle, surfaceCaps)
        vkCheck(result) { "Failed to update surface capabilities" }
    }

    val surfaceFormat: SurfaceFormat = memoryStack { stack ->
        val pCount = stack.mallocInt(1)
        vkCheck(KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.handle, handle, pCount, null)) {
            "Failed to get the number of surface formats"
        }

        val numFormats = pCount[0]
        check(numFormats > 0) { "No surface formats retrieved" }

        val surfaceFormats = VkSurfaceFormatKHR.calloc(numFormats, stack)
        vkCheck(KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.handle, handle, pCount, surfaceFormats)) {
            "Failed to get surface formats"
        }

        val formats = (0 until numFormats).map { i -> surfaceFormats[i] }
        val formatColorPairs: List<String> = formats.map {
            "${it.format().decodeFormat()}_${it.colorSpace().decodeColorSpace().replace("COLOR_SPACE_", "")}"
        }

        logger.debugGrid("Surface Formats [${formats.size}]", formatColorPairs) { pair ->
            pair.substringBefore("_")
        }

        val requirements = SurfaceSelector.getRequirements()
        val idealFormat = formats.maxBy { format ->
            requirements.sumOf { req ->
                when (val result = req.check(format)) {
                    is Boolean -> if (result) req.weight else 0
                    is Number -> result.toInt() * max(1, req.weight)
                    else -> 0
                }
            }
        }

        boxedString(boxCharset = BoxCharset.ROUNDED, title = "Selected Surface Format") {
            appendLine("Format: ${idealFormat.format().decodeFormat()}")
            appendLine("Color Space: ${idealFormat.colorSpace().decodeColorSpace()}")
            appendLine("Min Image Count: ${surfaceCaps.minImageCount()}")
            appendLine("Max Image Count: ${if (surfaceCaps.maxImageCount() == 0) "Unlimited" else surfaceCaps.maxImageCount()}")
            appendLine("Current Extent: ${surfaceCaps.currentExtent().width()}x${surfaceCaps.currentExtent().height()}")
        }.log(logger, Level.DEBUG)

        return@memoryStack SurfaceFormat(idealFormat.format(), idealFormat.colorSpace())
    }

    override fun destroy() {
        surfaceCaps.free()
        KHRSurface.vkDestroySurfaceKHR(instance.handle, handle, null)
    }
}
