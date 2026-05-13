package github.businessdirt.axite.vanadium.renderer.scene

import github.businessdirt.axite.vanadium.vulkan.resources.Buffer
import org.joml.Vector2f
import org.joml.Vector3f

data class Vertex(
    val position: Vector3f,
    val normal: Vector3f,
    val uv: Vector2f,
    val color: Vector3f = Vector3f(1f, 1f, 1f)
) {
    companion object {
        const val SIZE = (3 + 3 + 2 + 3) * 4 // Size in bytes
    }
}

class Mesh(
    val vertexBuffer: Buffer,
    val indexBuffer: Buffer,
    val indexCount: Int
) : AutoCloseable {
    override fun close() {
        vertexBuffer.close()
        indexBuffer.close()
    }
}
