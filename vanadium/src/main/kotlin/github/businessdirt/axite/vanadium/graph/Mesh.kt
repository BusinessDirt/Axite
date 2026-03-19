package github.businessdirt.axite.vanadium.graph

import github.businessdirt.axite.vanadium.platform.vulkan.resources.Buffer

data class Mesh(
    val id: String,
    val vertexBuffer: Buffer,
    val indexBuffer: Buffer,
    val indexCount: Int,
) {
    fun cleanup() {
        vertexBuffer.cleanup()
        indexBuffer.cleanup()
    }
}