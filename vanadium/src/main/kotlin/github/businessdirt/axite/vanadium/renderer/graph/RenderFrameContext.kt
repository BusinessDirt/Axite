package github.businessdirt.axite.vanadium.renderer.graph

class RenderFrameContext {
    private val writtenResources = mutableSetOf<String>()

    fun isFirstWrite(resourceName: String): Boolean {
        val first = resourceName !in writtenResources
        writtenResources.add(resourceName)
        return first
    }

    fun reset() {
        writtenResources.clear()
    }
}