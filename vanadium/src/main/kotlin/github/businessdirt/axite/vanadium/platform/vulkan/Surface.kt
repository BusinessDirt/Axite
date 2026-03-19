package github.businessdirt.axite.vanadium.platform.vulkan

import github.businessdirt.axite.vanadium.utils.createHandle
import github.businessdirt.axite.vanadium.utils.memoryStack
import github.businessdirt.axite.vanadium.utils.vkCheck
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.vulkan.KHRSurface
import org.lwjgl.vulkan.VK13.VK_FORMAT_B8G8R8A8_SRGB
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR
import org.lwjgl.vulkan.VkSurfaceFormatKHR

class Surface(
    physicalDevice: PhysicalDevice,
    private val instance: Instance,
    windowHandle: Long
) : VulkanHandle<Long>() {

    data class SurfaceFormat(val imageFormat: Int, val colorSpace: Int)

    override val handle: Long = memoryStack { stack ->
        stack.createHandle({ "Failed to create window surface" }) { longBuffer ->
            GLFWVulkan.glfwCreateWindowSurface(instance.handle, windowHandle, null, longBuffer)
        }
    }

    val surfaceCaps: VkSurfaceCapabilitiesKHR = VkSurfaceCapabilitiesKHR.calloc().also {
        vkCheck(KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice.handle, handle, it)) {
            "Failed to get surface capabilities"
        }
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

        val idealFormat = (0 until numFormats)
            .map { i -> surfaceFormats[i] }
            .firstOrNull { format ->
                format.format() == VK_FORMAT_B8G8R8A8_SRGB &&
                        format.colorSpace() == KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR
            }

        return@memoryStack if (idealFormat != null) {
            SurfaceFormat(idealFormat.format(), idealFormat.colorSpace())
        } else {
            SurfaceFormat(VK_FORMAT_B8G8R8A8_SRGB, surfaceFormats[0].colorSpace())
        }
    }

    override fun destroy() {
        surfaceCaps.free()
        KHRSurface.vkDestroySurfaceKHR(instance.handle, handle, null)
    }
}