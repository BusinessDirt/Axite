package github.businessdirt.axite.vanadium.graph

import github.businessdirt.axite.events.HandleEvent
import github.businessdirt.axite.vanadium.InitData
import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.VanadiumConfig
import github.businessdirt.axite.vanadium.events.glfw.WindowResizedEvent
import github.businessdirt.axite.vanadium.graph.scene.SceneRenderGraph
import github.businessdirt.axite.vanadium.platform.Window
import github.businessdirt.axite.vanadium.platform.vulkan.Context
import github.businessdirt.axite.vanadium.platform.vulkan.GraphicsQueue
import github.businessdirt.axite.vanadium.platform.vulkan.PresentQueue
import github.businessdirt.axite.vanadium.platform.vulkan.command.CommandBuffer
import github.businessdirt.axite.vanadium.platform.vulkan.command.CommandPool
import github.businessdirt.axite.vanadium.platform.vulkan.synchronization.Fence
import github.businessdirt.axite.vanadium.platform.vulkan.synchronization.Semaphore
import github.businessdirt.axite.vanadium.utils.VulkanUtils
import github.businessdirt.axite.vanadium.utils.memoryStack
import org.lwjgl.vulkan.VK13.VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT
import org.lwjgl.vulkan.VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT
import org.lwjgl.vulkan.VkCommandBufferSubmitInfo
import org.lwjgl.vulkan.VkExtent2D
import org.lwjgl.vulkan.VkSemaphoreSubmitInfo
import org.slf4j.LoggerFactory
import java.util.*


object RenderGraph {

    private val logger = LoggerFactory.getLogger(this::class.java)

    // Frame-specific synchronization and command resources
    private lateinit var commandPools: List<CommandPool>
    private lateinit var commandBuffers: List<CommandBuffer>
    private lateinit var fences: List<Fence>
    private lateinit var imageAvailableSemaphores: MutableList<Semaphore>
    private lateinit var renderFinishedSemaphores: MutableList<Semaphore>

    private lateinit var graphicsQueue: GraphicsQueue
    private lateinit var presentQueue: PresentQueue

    private var currentFrame = 0
    private var isInitialized = false
    private var resize = false

    private lateinit var sceneRenderGraph: SceneRenderGraph
    private lateinit var modelCache: ModelCache

    private lateinit var window: Window
    private lateinit var config: VanadiumConfig

    fun initialize(window: Window, config: VanadiumConfig, initData: InitData) {
        if (isInitialized) return

        Context.initialize(window, config)

        graphicsQueue = GraphicsQueue()
        presentQueue = PresentQueue()

        this.window = window
        this.config = config

        commandPools = List(VulkanUtils.MAX_FRAMES_IN_FLIGHT) {
            CommandPool(graphicsQueue.queueFamilyIndex)
        }

        commandBuffers = List(VulkanUtils.MAX_FRAMES_IN_FLIGHT) { i ->
            CommandBuffer(commandPools[i], primary = true, oneTimeSubmit = true)
        }

        fences = List(VulkanUtils.MAX_FRAMES_IN_FLIGHT) {
            Fence(signaled = true)
        }

        imageAvailableSemaphores = MutableList(VulkanUtils.MAX_FRAMES_IN_FLIGHT) {
            Semaphore()
        }

        renderFinishedSemaphores = MutableList(Context.swapChain.imageCount) {
            Semaphore()
        }

        sceneRenderGraph = SceneRenderGraph(config)

        modelCache = ModelCache()
        logger.debug("Loading {} model(s)", initData.models.size)
        modelCache.loadModels(initData.models, commandPools[0], graphicsQueue)
        logger.debug("Loaded {} model(s)", initData.models.size)

        isInitialized = true
    }

    fun render() {
        val fence = fences[currentFrame]
        val imageAvailable = imageAvailableSemaphores[currentFrame]

        fence.wait()

        // This blocks until the semaphore is ready to be signalled by the Swap Chain
        val imageIndex = Context.swapChain.acquireNextImage(imageAvailable)
        if (resize || imageIndex < 0) return

        // Reset resources and record
        commandPools[currentFrame].reset()
        val commandBuffer = commandBuffers[currentFrame]

        commandBuffer.record {
            sceneRenderGraph.render(this, modelCache, imageIndex)
        }

        // Submit to Graphics Queue
        memoryStack { stack ->
            fence.reset()

            val commands = VkCommandBufferSubmitInfo.calloc(1, stack).`sType$Default`()
                .commandBuffer(commandBuffer.handle)

            val waitSemaphores = VkSemaphoreSubmitInfo.calloc(1, stack).`sType$Default`()
                .semaphore(imageAvailable.handle)
                .stageMask(VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT)

            val signalSemaphores = VkSemaphoreSubmitInfo.calloc(1, stack).`sType$Default`()
                .semaphore(renderFinishedSemaphores[imageIndex].handle)
                .stageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)

            graphicsQueue.submit(commands, waitSemaphores, signalSemaphores, fence)
        }

        resize = Context.swapChain.present(
            presentQueue,
            renderFinishedSemaphores[imageIndex],
            imageIndex
        )

        currentFrame = (currentFrame + 1) % VulkanUtils.MAX_FRAMES_IN_FLIGHT
    }

    @HandleEvent(priority = Int.MIN_VALUE)
    fun onWindowResizedEvent(event: WindowResizedEvent) {
        if (event.width == 0 && event.height == 0) return

        resize = false
        Context.device.waitIdle()
        Context.resize(window, config)

        renderFinishedSemaphores.forEach(Semaphore::cleanup)
        imageAvailableSemaphores.forEach(Semaphore::cleanup)

        for (i in 0..<VulkanUtils.MAX_FRAMES_IN_FLIGHT) imageAvailableSemaphores[i] = Semaphore()
        for (i in 0..<Context.swapChain.imageCount) renderFinishedSemaphores[i] = Semaphore()

        val extent: VkExtent2D = Context.swapChain.extent
        Vanadium.scene.projection.resize(extent.width(), extent.height())
        sceneRenderGraph.resize()
    }

    fun shutdown() {
        if (!isInitialized) return

        Context.device.waitIdle()

        modelCache.cleanup()
        sceneRenderGraph.cleanup()

        // Clean up frame resources
        imageAvailableSemaphores.forEach { it.cleanup() }
        renderFinishedSemaphores.forEach { it.cleanup() }
        fences.forEach { it.cleanup() }

        commandPools.forEachIndexed { i, pool ->
            commandBuffers[i].cleanup()
            pool.cleanup()
        }

        // Finally, shut down the core context
        Context.shutdown()
        isInitialized = false
    }
}