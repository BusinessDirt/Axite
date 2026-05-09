package github.businessdirt.axite.vanadium.vulkan

import github.businessdirt.axite.vanadium.VanadiumConfig
import github.businessdirt.axite.vanadium.core.profiling.Profiler
import github.businessdirt.axite.vanadium.core.utils.VulkanUtils.coerceRequestedImageCount
import github.businessdirt.axite.vanadium.platform.Window
import github.businessdirt.axite.vanadium.vulkan.device.Device
import github.businessdirt.axite.vanadium.vulkan.device.GraphicsQueue
import github.businessdirt.axite.vanadium.vulkan.device.PhysicalDevice
import github.businessdirt.axite.vanadium.vulkan.device.PresentQueue
import github.businessdirt.axite.vanadium.vulkan.device.pickPhysicalDevice
import github.businessdirt.axite.vanadium.vulkan.pipeline.PipelineCache
import github.businessdirt.axite.vanadium.vulkan.surface.Surface
import github.businessdirt.axite.vanadium.vulkan.swapchain.Swapchain
import kotlin.math.min

class Context(private val config: VanadiumConfig) {

    private val scope = ResourceScope()

    lateinit var instance: Instance
        private set

    lateinit var debugMessenger: DebugMessenger
        private set

    lateinit var physicalDevice: PhysicalDevice
        private set

    lateinit var device: Device
        private set

    lateinit var surface: Surface
        private set

    lateinit var graphicsQueue: GraphicsQueue
        private set

    lateinit var presentQueue: PresentQueue
        private set

    lateinit var swapchain: Swapchain
        private set

    lateinit var pipelineCache: PipelineCache
        private set

    lateinit var frameData: Array<FrameData>
        private set

    var maxFramesInFlight: Int = 0
        private set

    var currentFrameIndex: Int = 0
        private set

    val currentFrameData: FrameData
        get() = frameData[currentFrameIndex]

    fun initialize(window: Window) = Profiler.profile("Vulkan Context Initialization") {
        instance = scope.use(Instance(config))
        if (config.validate) debugMessenger = scope.use(DebugMessenger(instance.handle))
        physicalDevice = scope.use(instance.pickPhysicalDevice())
        device = scope.use(Device(physicalDevice))
        surface = scope.use(Surface(physicalDevice, instance, window.handle))
        graphicsQueue = scope.use(GraphicsQueue(device.handle, physicalDevice))
        presentQueue = scope.use(PresentQueue(device.handle, physicalDevice, surface))

        val requestedImages = surface.surfaceCaps.coerceRequestedImageCount(config.requestedImages)
        swapchain = scope.use(Swapchain(device, physicalDevice, window, surface, requestedImages, config.vsync))

        pipelineCache = scope.use(PipelineCache(device.handle))

        // Determine max frames in flight. Standard is 2, but we must not exceed (imageCount - 1)
        // to avoid stalling on image acquisition.
        maxFramesInFlight = min(2, swapchain.imageCount - 1)

        // Initialize frames in flight
        frameData = Array(maxFramesInFlight) {
            scope.use(FrameData(device, graphicsQueue.queueFamilyIndex))
        }
    }

    fun nextFrame() {
        currentFrameData.destroyTransientResources()
        currentFrameIndex = (currentFrameIndex + 1) % maxFramesInFlight
    }

    fun shutdown() = Profiler.profile("Vulkan Context Shutdown") {
        device.waitIdle()
        scope.close()
    }

    fun resize() {
        device.waitIdle()
        swapchain.recreate()
    }
}
