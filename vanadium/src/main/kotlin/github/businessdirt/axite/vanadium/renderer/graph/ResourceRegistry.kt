package github.businessdirt.axite.vanadium.renderer.graph

import github.businessdirt.axite.vanadium.vulkan.Context
import github.businessdirt.axite.vanadium.vulkan.resources.Attachment
import github.businessdirt.axite.vanadium.vulkan.resources.Image

class ResourceRegistry(val context: Context) {
    // Persistent resources (re-created only on window resize)
    private val persistentAttachments = mutableMapOf<String, Attachment>()

    fun importResource(name: String, image: Image, width: Int, height: Int, format: Int, usage: Int) {
        persistentAttachments[name]?.close()
        persistentAttachments[name] = Attachment(context.device.handle, context.physicalDevice, width, height, format, usage, image)
    }

    fun createResource(name: String, width: Int, height: Int, format: Int, usage: Int) {
        persistentAttachments[name]?.close()
        persistentAttachments[name] = Attachment(context.device.handle, context.physicalDevice, width, height, format, usage)
    }

    fun removeResource(name: String) {
        persistentAttachments.remove(name)?.close()
    }

    fun get(name: String): Attachment = persistentAttachments[name]
        ?: throw IllegalStateException("Resource $name not registered!")

    fun hasResource(name: String): Boolean = persistentAttachments.containsKey(name)

    fun clear() {
        persistentAttachments.values.forEach { it.close() }
        persistentAttachments.clear()
    }
}