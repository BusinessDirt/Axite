package github.businessdirt.axite.vanadium.renderer.graph

import github.businessdirt.axite.vanadium.vulkan.Context
import github.businessdirt.axite.vanadium.vulkan.resources.Image

class ResourceRegistry(val context: Context) {
    // Persistent resources (re-created only on window resize)
    private val persistentImages = mutableMapOf<String, Image>()

    // Transient resources (reused via aliasing - advanced)
    // TODO: private val transientPool = ...

    fun registerPersistent(name: String, image: Image) {
        persistentImages[name] = image
    }

    fun get(name: String): Image = persistentImages[name]
        ?: throw IllegalStateException("Resource $name not registered!")
}