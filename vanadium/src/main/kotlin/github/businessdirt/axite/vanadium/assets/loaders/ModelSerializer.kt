package github.businessdirt.axite.vanadium.assets.loaders

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.assets.metadata.ModelMetadata
import github.businessdirt.axite.vanadium.assets.types.Model
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.scene.Mesh
import github.businessdirt.axite.vanadium.scene.Vertex
import github.businessdirt.axite.vanadium.vulkan.resources.Buffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.serializer
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.assimp.AIMesh
import org.lwjgl.assimp.Assimp.*
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK13.*
import java.io.File
import java.nio.ByteBuffer

class ModelSerializer : AssetSerializer<Model, ModelMetadata>(
    serializer<ModelMetadata>()
) {

    override suspend fun load(path: String): Model = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) throw IllegalArgumentException("Model file not found: $path")

        val scene = aiImportFile(path, aiProcess_Triangulate or aiProcess_FlipUVs or aiProcess_CalcTangentSpace)
            ?: throw RuntimeException("Failed to load model: [${aiGetErrorString()}]")

        val metadata = loadMetadata(path) ?: ModelMetadata(meshCount = scene.mNumMeshes())

        val meshes = mutableListOf<Mesh>()
        for (i in 0 until scene.mNumMeshes()) {
            val aiMesh = AIMesh.create(scene.mMeshes()!![i])
            meshes.add(loadMesh(aiMesh))
        }

        aiReleaseImport(scene)

        val finalMetadata = metadata.copy(meshCount = meshes.size)
        Model(path, finalMetadata.uuid, finalMetadata, meshes)
    }

    private fun loadMesh(aiMesh: AIMesh): Mesh = memoryStack {
        val vertexCount = aiMesh.mNumVertices()
        val vertices = mutableListOf<Vertex>()

        for (i in 0 until vertexCount) {
            val pos = aiMesh.mVertices()[i].let { Vector3f(it.x(), it.y(), it.z()) }
            val normal = aiMesh.mNormals()!![i].let { Vector3f(it.x(), it.y(), it.z()) }
            val uv = if (aiMesh.mTextureCoords(0) != null) {
                aiMesh.mTextureCoords(0)!![i].let { Vector2f(it.x(), it.y()) }
            } else {
                Vector2f(0f, 0f)
            }
            vertices.add(Vertex(pos, normal, uv))
        }

        val indexCount = aiMesh.mNumFaces() * 3
        val indices = IntArray(indexCount)
        for (i in 0 until aiMesh.mNumFaces()) {
            val face = aiMesh.mFaces()[i]
            indices[i * 3 + 0] = face.mIndices()[0]
            indices[i * 3 + 1] = face.mIndices()[1]
            indices[i * 3 + 2] = face.mIndices()[2]
        }

        val vertexBufferSize = (vertexCount * Vertex.SIZE).toLong()
        val indexBufferSize = (indexCount * 4).toLong()

        // Create buffers
        val vertexBuffer = createDeviceLocalBuffer(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, vertexBufferSize) { buffer ->
            for (v in vertices) {
                buffer.putFloat(v.position.x).putFloat(v.position.y).putFloat(v.position.z)
                buffer.putFloat(v.normal.x).putFloat(v.normal.y).putFloat(v.normal.z)
                buffer.putFloat(v.uv.x).putFloat(v.uv.y)
                buffer.putFloat(v.color.x).putFloat(v.color.y).putFloat(v.color.z)
            }
        }

        val indexBuffer = createDeviceLocalBuffer(VK_BUFFER_USAGE_INDEX_BUFFER_BIT, indexBufferSize) { buffer ->
            for (idx in indices) {
                buffer.putInt(idx)
            }
        }

        Mesh(vertexBuffer, indexBuffer, indexCount)
    }

    private fun createDeviceLocalBuffer(usage: Int, size: Long, fillBlock: (ByteBuffer) -> Unit): Buffer {
        val stagingBuffer = Buffer(
            Vanadium.context.device.handle,
            Vanadium.context.physicalDevice,
            size,
            VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
        )

        val pMapped = stagingBuffer.map()
        val buffer = MemoryUtil.memByteBuffer(pMapped, size.toInt())
        fillBlock(buffer)
        stagingBuffer.unmap()

        val deviceBuffer = Buffer(
            Vanadium.context.device.handle,
            Vanadium.context.physicalDevice,
            size,
            usage or VK_BUFFER_USAGE_TRANSFER_DST_BIT,
            VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
        )

        Vanadium.context.graphicsQueue.execute {
            memoryStack { stack ->
                val copyRegion = org.lwjgl.vulkan.VkBufferCopy2.calloc(1, stack).`sType$Default`()
                    .srcOffset(0)
                    .dstOffset(0)
                    .size(size)

                val copyInfo = org.lwjgl.vulkan.VkCopyBufferInfo2.calloc(stack).`sType$Default`()
                    .srcBuffer(stagingBuffer.handle)
                    .dstBuffer(deviceBuffer.handle)
                    .pRegions(copyRegion)

                vkCmdCopyBuffer2(handle, copyInfo)
            }
        }

        stagingBuffer.close()
        return deviceBuffer
    }
}
