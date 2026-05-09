package github.businessdirt.axite.vanadium.renderer

import github.businessdirt.axite.vanadium.VanadiumAdapter
import github.businessdirt.axite.vanadium.core.profiling.Profiler
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.renderer.graph.RenderGraph
import github.businessdirt.axite.vanadium.renderer.graph.transitionLayout
import github.businessdirt.axite.vanadium.vulkan.Context
import github.businessdirt.axite.vanadium.vulkan.resources.Image
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkCommandBufferSubmitInfo
import org.lwjgl.vulkan.VkSemaphoreSubmitInfo

class Renderer(val context: Context) {

    private val renderGraph = RenderGraph(context)
    private val sceneRenderer = SceneRenderer(context)
    private var resize = false

    fun initialize() = Profiler.profile("Renderer Initialization"){
        sceneRenderer.initialize()
    }

    fun shutdown() = Profiler.profile("Renderer Shutdown") {
        context.device.waitIdle()
        sceneRenderer.shutdown()
        renderGraph.clear()
    }

    fun render(adapter: VanadiumAdapter, interpolation: Double) {
        val currentFrameData = context.currentFrameData
        currentFrameData.inFlightFence.wait()

        val imageIndex = context.swapchain.acquireNextImage(currentFrameData.imageAvailableSemaphore)
        if (resize || imageIndex < 0) return

        currentFrameData.commandPool.reset()

        // Prepare Resources
        // Prepare Resources
        val extent = context.swapchain.extent

        // 1. Swapchain Image (update handle every frame)
        if (!renderGraph.registry.hasResource("SwapChainImage")) {
            val swapchainImage = Image(context.device.handle, context.physicalDevice, context.swapchain.imageViews[imageIndex].imageHandle) {
                this.width = extent.width()
                this.height = extent.height()
                this.format = context.swapchain.surface.surfaceFormat.imageFormat
                this.usage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
            }
            renderGraph.importResource("SwapChainImage", swapchainImage, extent.width(), extent.height(), swapchainImage.format, swapchainImage.data.usage)
        } else {
            val attachment = renderGraph.registry.get("SwapChainImage")
            // Update the handle of the existing image wrapper
            // Note: We need a way to update the handle in the Image class
            attachment.updateImageHandle(context.swapchain.imageViews[imageIndex].imageHandle)
        }


        // Depth Buffer (created once or on resize)
        if (resize || !renderGraph.registry.hasResource("DepthBuffer")) {
            renderGraph.createResource("DepthBuffer", extent.width(), extent.height(), VK_FORMAT_D32_SFLOAT, VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT)
        }


        // 2. Record/Build the frame
        currentFrameData.commandBuffer.record {
            renderGraph.use(this) {
                adapter.onRecord(it, sceneRenderer, this, interpolation)
            }

            // Final transition for presentation
            val swapchainAttachment = renderGraph.registry.get("SwapChainImage")
            transitionLayout(swapchainAttachment, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
        }


        memoryStack { stack ->
            currentFrameData.inFlightFence.reset()

            val commands = VkCommandBufferSubmitInfo.calloc(1, stack).`sType$Default`()
                .commandBuffer(currentFrameData.commandBuffer.handle)

            val waitSemaphores = VkSemaphoreSubmitInfo.calloc(1, stack).`sType$Default`()
                .semaphore(currentFrameData.imageAvailableSemaphore.handle)
                .stageMask(VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT)

            val signalSemaphore = VkSemaphoreSubmitInfo.calloc(1, stack).`sType$Default`()
                .semaphore(context.swapchain.renderFinishedSemaphores[imageIndex].handle)
                .stageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)

            context.graphicsQueue.submit(commands, waitSemaphores, signalSemaphore, currentFrameData.inFlightFence)
        }

        resize = context.swapchain.present(context.presentQueue, context.swapchain.renderFinishedSemaphores[imageIndex], imageIndex)

        context.nextFrame()
    }
}