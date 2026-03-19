package github.businessdirt.axite.vanadium.platform.vulkan

import github.businessdirt.axite.vanadium.platform.Window
import github.businessdirt.axite.vanadium.platform.vulkan.resources.ImageView
import github.businessdirt.axite.vanadium.platform.vulkan.synchronization.Semaphore
import github.businessdirt.axite.vanadium.utils.createHandle
import github.businessdirt.axite.vanadium.utils.getInt
import github.businessdirt.axite.vanadium.utils.memoryStack
import github.businessdirt.axite.vanadium.utils.vkCheck
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkExtent2D
import org.lwjgl.vulkan.VkPresentInfoKHR
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR

class SwapChain(
    window: Window,
    device: Device,
    surface: Surface,
    requestedImages: Int,
    vsync: Boolean
) : VulkanHandle<Long>() {

    val swapChainExtent: VkExtent2D = surface.surfaceCaps.calculateSwapChainExtent(window)

    override val handle: Long = memoryStack { stack ->
        val surfaceCaps = surface.surfaceCaps
        val surfaceFormat = surface.surfaceFormat
        val vkSwapChainCreateInfo = VkSwapchainCreateInfoKHR.calloc(stack)
            .`sType$Default`()
            .surface(surface.handle)
            .minImageCount(surfaceCaps.calculateNumberOfImages(requestedImages))
            .imageFormat(surfaceFormat.imageFormat)
            .imageColorSpace(surfaceFormat.colorSpace)
            .imageExtent(swapChainExtent)
            .imageArrayLayers(1)
            .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
            .preTransform(surfaceCaps.currentTransform())
            .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
            .clipped(true)
            .presentMode(if (vsync) VK_PRESENT_MODE_FIFO_KHR else VK_PRESENT_MODE_IMMEDIATE_KHR)

        stack.createHandle({ "Failed to create swap chain" }) { longBuffer ->
            vkCreateSwapchainKHR(device.handle, vkSwapChainCreateInfo, null, longBuffer)
        }
    }

    val imageViews: Array<ImageView> = memoryStack { stack ->
        stack.createImageViews(device, handle, surface.surfaceFormat.imageFormat)
    }

    val imageCount: Int = imageViews.size

    fun acquireNextImage(imageAvailableSemaphore: Semaphore, timeout: Long = Long.MAX_VALUE): Int = memoryStack { stack ->
        val pIndex = stack.mallocInt(1)
        val err = vkAcquireNextImageKHR(Context.device.handle, handle, timeout,
            imageAvailableSemaphore.handle, MemoryUtil.NULL, pIndex)

        when (err) {
            VK_SUCCESS, VK_SUBOPTIMAL_KHR -> pIndex[0]
            VK_ERROR_OUT_OF_DATE_KHR -> -1
            else -> throw RuntimeException("Failed to acquire image: $err")
        }
    }

    fun present(queue: PresentQueue, renderFinishedSemaphore: Semaphore, imageIndex: Int): Boolean = memoryStack { stack ->
        val presentInfo = VkPresentInfoKHR.calloc(stack).`sType$Default`()
            .pWaitSemaphores(stack.longs(renderFinishedSemaphore.handle))
            .swapchainCount(1)
            .pSwapchains(stack.longs(handle))
            .pImageIndices(stack.ints(imageIndex))

        when (val err = vkQueuePresentKHR(queue.handle, presentInfo)) {
            VK_SUCCESS, VK_SUBOPTIMAL_KHR -> false
            VK_ERROR_OUT_OF_DATE_KHR -> true
            else -> throw RuntimeException("Failed to present KHR: $err")
        }
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
            ImageView(swapChainImages[i]) {
                this.format = format
                this.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT
            }
        }
    }

    override fun destroy() {
        swapChainExtent.free()
        imageViews.forEach { it.cleanup() }
        vkDestroySwapchainKHR(Context.device.handle, handle, null)
    }
}