package github.businessdirt.axite.vanadium.vulkan

import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer
import github.businessdirt.axite.vanadium.vulkan.commands.CommandPool
import github.businessdirt.axite.vanadium.vulkan.device.Device
import github.businessdirt.axite.vanadium.vulkan.synchronization.Fence
import github.businessdirt.axite.vanadium.vulkan.synchronization.Semaphore

/**
 * Encapsulates all resources required for a single "frame in flight".
 */
class FrameData(
    device: Device,
    queueFamilyIndex: Int
) : Handle<Unit>() {

    override val handle: Unit = Unit

    /**
     * Dedicated command pool for this frame.
     * We enable [supportReset] so we can reuse the command buffer every frame.
     */
    val commandPool = CommandPool(device.handle, queueFamilyIndex, supportReset = true)

    /**
     * Primary command buffer used for main rendering commands.
     */
    val commandBuffer: CommandBuffer = commandPool.allocate(primary = true)

    /**
     * Semaphore signaled when the swapchain has provided an image for us to render into.
     */
    val imageAvailableSemaphore = Semaphore(device.handle)

    /**
     * Semaphore signaled when rendering is complete and the image is ready for presentation.
     */
    val renderFinishedSemaphore = Semaphore(device.handle)

    /**
     * Fence used to synchronize the CPU with the GPU.
     * Initialized in the [signaled] state so the first frame doesn't wait indefinitely.
     */
    val inFlightFence = Fence(device.handle, signaled = true)

    private val transientResources = mutableListOf<AutoCloseable>()

    fun <T : AutoCloseable> track(resource: T): T {
        transientResources.add(resource)
        return resource
    }

    fun destroyTransientResources() {
        transientResources.reversed().forEach { it.close() }
        transientResources.clear()
    }

    override fun destroy() {
        destroyTransientResources()

        commandPool.close()
        imageAvailableSemaphore.close()
        renderFinishedSemaphore.close()
        inFlightFence.close()
    }
}
