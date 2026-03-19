package github.businessdirt.axite.vanadium.platform.vulkan

import github.businessdirt.axite.vanadium.platform.Window
import github.businessdirt.axite.vanadium.platform.vulkan.resources.ImageView
import github.businessdirt.axite.vanadium.utils.vkCheck

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK13.*

class SwapChain(
    val window: Window,
    val device: Device,
    val surface: Surface,
    requestedImages: Int,
    vsync: Boolean
) : VulkanHandle<Long>() {

    val imageViews: Array<ImageView>
    val numImages: Int
    val swapChainExtent: VkExtent2D

    override val handle: Long = MemoryStack.stackPush().use { stack ->
        val surfaceCaps = surface.surfaceCaps

        val reqImages = surfaceCaps.calculateNumberOfImages(requestedImages)
        swapChainExtent = surfaceCaps.calculateSwapChainExtent(window)

        val surfaceFormat = surface.surfaceFormat
        val vkSwapChainCreateInfo = VkSwapchainCreateInfoKHR.calloc(stack)
            .`sType$Default`()
            .surface(surface.handle)
            .minImageCount(reqImages)
            .imageFormat(surfaceFormat.imageFormat)
            .imageColorSpace(surfaceFormat.colorSpace)
            .imageExtent(swapChainExtent)
            .imageArrayLayers(1)
            .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
            .preTransform(surfaceCaps.currentTransform())
            .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
            .clipped(true)
            .presentMode(if (vsync) VK_PRESENT_MODE_FIFO_KHR else VK_PRESENT_MODE_IMMEDIATE_KHR)

        val lp = stack.mallocLong(1)
        vkCheck(vkCreateSwapchainKHR(device.handle, vkSwapChainCreateInfo, null, lp)) {
            "Failed to create swap chain"
        }
        val swapChainHandle = lp[0]

        imageViews = stack.createImageViews(device, swapChainHandle, surfaceFormat.imageFormat)
        numImages = imageViews.size

        swapChainHandle
    }

    private fun VkSurfaceCapabilitiesKHR.calculateNumberOfImages(requestedImages: Int): Int {
        val max = if (maxImageCount() > 0) maxImageCount() else Int.MAX_VALUE
        return requestedImages.coerceIn(minImageCount(), max).also { result ->
            logger.debug("Requested [$requestedImages] images, got [$result] images. Max: [${maxImageCount()}], Min: [${minImageCount()}]")
        }
    }

    private fun VkSurfaceCapabilitiesKHR.calculateSwapChainExtent(window: Window): VkExtent2D =
        VkExtent2D.calloc().apply {
            if (currentExtent().width() != -1) {
                set(currentExtent())
            } else {
                width(window.width.coerceIn(minImageExtent().width(), maxImageExtent().width()))
                height(window.height.coerceIn(minImageExtent().height(), maxImageExtent().height()))
            }
        }

    private fun MemoryStack.createImageViews(device: Device, swapChain: Long, format: Int): Array<ImageView> {
        val ip = mallocInt(1)
        vkCheck(vkGetSwapchainImagesKHR(device.handle, swapChain, ip, null)) {
            "Failed to get number of surface images"
        }

        val count = ip[0]
        val swapChainImages = mallocLong(count)
        vkCheck(vkGetSwapchainImagesKHR(device.handle, swapChain, ip, swapChainImages)) {
            "Failed to get surface images"
        }

        return Array(count) { i ->
            ImageView(device, swapChainImages[i]) {
                this.format = format
                this.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT
            }
        }
    }

    override fun destroy() {
        swapChainExtent.free()
        imageViews.forEach { it.destroy() }
        vkDestroySwapchainKHR(device.handle, handle, null)
    }
}