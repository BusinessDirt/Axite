package github.businessdirt.axite.vanadium.renderer.graph

import github.businessdirt.axite.vanadium.vulkan.Context
import github.businessdirt.axite.vanadium.vulkan.resources.Attachment

object RenderResourceNames {
    const val BACK_BUFFER = "swapchain_color"
    const val DEPTH_BUFFER = "swapchain_depth"
}

class ResourceRegistry(val context: Context) {
    // Resources that stay the same unless the window is resized
    private val persistentAttachments = mutableMapOf<String, Attachment>()

    // Resources that change every frame (e.g., current Swapchain image)
    private val frameResources = mutableMapOf<String, Attachment>()

    /**
     * Binds a resource specifically for the duration of the current frame.
     * These are cleared or overwritten every time the render loop runs.
     */
    fun bindFrameResource(name: String, attachment: Attachment) {
        frameResources[name] = attachment
    }

    fun forceRecreateResource(name: String, width: Int, height: Int, format: Int, usage: Int) {
        persistentAttachments[name]?.close()
        persistentAttachments[name] = Attachment(context.device.handle, context.physicalDevice, width, height, format, usage)
    }

    fun ensureResource(name: String, width: Int, height: Int, format: Int, usage: Int) {
        if (!persistentAttachments.containsKey(name)) {
            persistentAttachments[name] = Attachment(context.device.handle, context.physicalDevice, width, height, format, usage)
        }
    }

    fun ensureResourceMatches(
        name: String,
        width: Int,
        height: Int,
        format: Int,
        usage: Int,
        shouldRecreate: (Attachment) -> Boolean = { old ->
            old.width != width || old.height != height || old.format != format || old.usage != usage
        }
    ) {
        val existing = persistentAttachments[name]
        if (existing == null || shouldRecreate(existing)) {
            existing?.close()
            persistentAttachments[name] = Attachment(context.device.handle, context.physicalDevice, width, height, format, usage)
        }
    }

    fun ensureResourceMatchesBackbuffer(
        name: String,
        usage: Int,
        shouldRecreate: (Attachment, Attachment) -> Boolean = { oldResource, currentBackbuffer ->
            oldResource.width != currentBackbuffer.width ||
                    oldResource.height != currentBackbuffer.height ||
                    oldResource.format != currentBackbuffer.format
        }
    ) {
        val existing = persistentAttachments[name]
        val backbuffer = frameResources[RenderResourceNames.BACK_BUFFER] ?: error("Backbuffer does not exist")

        if (existing == null || shouldRecreate(existing, backbuffer)) {
            existing?.close()
            persistentAttachments[name] = Attachment(
                context.device.handle,
                context.physicalDevice,
                backbuffer.width,
                backbuffer.height,
                backbuffer.format,
                usage
            )
        }
    }

    /**
     * Resolves a resource name.
     * Prioritizes the current frame's resources (like the backbuffer)
     * before checking persistent ones.
     */
    operator fun get(name: String): Attachment = frameResources[name]
        ?: persistentAttachments[name]
        ?: throw IllegalStateException("Resource '$name' not registered in Registry!")

    /**
     * Should be called at the start of each frame to ensure
     * old frame-specific references don't leak into the next frame.
     */
    fun prepareForFrame() = frameResources.clear()

    fun clear() {
        persistentAttachments.values.forEach { it.close() }
        persistentAttachments.clear()
        frameResources.clear()
    }
}