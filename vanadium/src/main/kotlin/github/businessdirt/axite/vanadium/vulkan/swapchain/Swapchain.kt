package github.businessdirt.axite.vanadium.vulkan.swapchain

import github.businessdirt.axite.vanadium.core.utils.VulkanUtils.coerceRequestedImageCount
import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.core.utils.vkCheck
import github.businessdirt.axite.vanadium.platform.Window
import github.businessdirt.axite.vanadium.vulkan.Handle
import github.businessdirt.axite.vanadium.vulkan.device.Device
import github.businessdirt.axite.vanadium.vulkan.device.PhysicalDevice
import github.businessdirt.axite.vanadium.vulkan.device.PresentQueue
import github.businessdirt.axite.vanadium.vulkan.resources.Attachment
import github.businessdirt.axite.vanadium.vulkan.resources.Image
import github.businessdirt.axite.vanadium.vulkan.resources.ImageView
import github.businessdirt.axite.vanadium.vulkan.surface.Surface
import github.businessdirt.axite.vanadium.vulkan.synchronization.Semaphore
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkExtent2D
import org.lwjgl.vulkan.VkPresentInfoKHR
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR
import kotlin.apply

class Swapchain(
    private val device: Device,
    private val physicalDevice: PhysicalDevice,
    private val window: Window,
    val surface: Surface,
    private var requestedImages: Int,
    private var vsync: Boolean
) : Handle<Long>() {

    override var handle: Long = 0L
        private set

    lateinit var extent: VkExtent2D
        private set

    lateinit var imageViews: Array<ImageView>
        private set

    lateinit var colorAttachments: Array<Attachment>
        private set

    lateinit var depthAttachment: Attachment
        private set

    lateinit var renderFinishedSemaphores: Array<Semaphore>
        private set

    var imageCount: Int = 0
        private set

    init {
        create()
    }

    private fun create() {
        extent = surface.surfaceCaps.calculateSwapChainExtent(window)
        handle = memoryStack { stack ->
            val surfaceCaps = surface.surfaceCaps
            val surfaceFormat = surface.surfaceFormat
            val vkSwapChainCreateInfo = VkSwapchainCreateInfoKHR.calloc(stack).`sType$Default`()
                .surface(surface.handle)
                .minImageCount(requestedImages)
                .imageFormat(surfaceFormat.imageFormat)
                .imageColorSpace(surfaceFormat.colorSpace)
                .imageExtent(extent)
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

        val imageHandles: LongArray = memoryStack { stack ->
            val pSwapchainImageCount = stack.mallocInt(1)
            vkCheck(vkGetSwapchainImagesKHR(device.handle, handle, pSwapchainImageCount, null)) {
                "Failed to get number of surface images"
            }

            val swapchainImageCount = pSwapchainImageCount[0]
            val pSwapchainImages = stack.mallocLong(swapchainImageCount)
            vkCheck(vkGetSwapchainImagesKHR(device.handle, handle, pSwapchainImageCount, pSwapchainImages)) {
                "Failed to get surface images"
            }

            LongArray(swapchainImageCount) { pSwapchainImages[it] }
        }

        imageViews = Array(imageHandles.size) { i ->
            ImageView(device.handle, imageHandles[i]) {
                this.format = surface.surfaceFormat.imageFormat
                this.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT
            }
        }

        // Wrap swapchain images in Attachment objects
        colorAttachments = Array(imageViews.size) { i ->
            val image = Image(device.handle, physicalDevice, existingHandle = imageHandles[i]) {
                this.width = extent.width()
                this.height = extent.height()
                this.format = surface.surfaceFormat.imageFormat
            }

            Attachment(
                device.handle, physicalDevice,
                extent.width(), extent.height(),
                surface.surfaceFormat.imageFormat,
                VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
                image
            )
        }

        depthAttachment = Attachment(
            device.handle, physicalDevice,
            extent.width(), extent.height(),
            VK_FORMAT_D32_SFLOAT,
            VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT
        )

        renderFinishedSemaphores = Array(imageViews.size) { Semaphore(device.handle) }

        imageCount = imageViews.size
    }

    fun acquireNextImage(imageAvailableSemaphore: Semaphore, timeout: Long = Long.MAX_VALUE): Int = memoryStack { stack ->
        val pIndex = stack.mallocInt(1)
        val err = vkAcquireNextImageKHR(device.handle, handle, timeout,
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

    private fun VkSurfaceCapabilitiesKHR.calculateSwapChainExtent(window: Window): VkExtent2D = VkExtent2D.calloc().apply {
        if (currentExtent().width() != -1) {
            set(currentExtent())
        } else {
            width(window.data.framebufferWidth.coerceIn(minImageExtent().width(), maxImageExtent().width()))
            height(window.data.framebufferHeight.coerceIn(minImageExtent().height(), maxImageExtent().height()))
        }
    }

    fun recreate(
        requestedImages: Int = this.requestedImages,
        vsync: Boolean = this.vsync
    ) {
        when {
            this.requestedImages != requestedImages -> this.requestedImages = surface.surfaceCaps.coerceRequestedImageCount(requestedImages)
            else -> this.requestedImages = requestedImages
        }

        this.vsync = vsync

        device.waitIdle()
        destroy()

        surface.updateCaps(physicalDevice)
        create()

        logger.atDebug().log("Recreated swapchain with [$requestedImages] requested images]")
    }

    override fun destroy() {
        if (handle == 0L) return

        if (::colorAttachments.isInitialized) colorAttachments.forEach { it.close() }
        if (::depthAttachment.isInitialized) depthAttachment.close()

        renderFinishedSemaphores.forEach { it.close() }
        imageViews.forEach { it.close() }
        extent.free()
        vkDestroySwapchainKHR(device.handle, handle, null)
        handle = 0L
    }
}
