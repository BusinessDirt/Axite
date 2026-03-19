package github.businessdirt.axite.vanadium.graph

import github.businessdirt.axite.vanadium.assets.model.MeshData
import github.businessdirt.axite.vanadium.assets.model.ModelData
import github.businessdirt.axite.vanadium.platform.vulkan.DeviceQueue
import github.businessdirt.axite.vanadium.platform.vulkan.command.CommandBuffer
import github.businessdirt.axite.vanadium.platform.vulkan.command.CommandPool
import github.businessdirt.axite.vanadium.platform.vulkan.resources.Buffer
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK13.*

class ModelCache {

    val modelMap = mutableMapOf<String, Model>()

    operator fun get(modelName: String): Model? = modelMap[modelName]

    fun loadModels(models: List<ModelData>, commandPool: CommandPool, queue: DeviceQueue) {
        val stagingBuffers = mutableListOf<Buffer>()
        val cmd = CommandBuffer(commandPool, primary = true, oneTimeSubmit = true)

        cmd.record {
            models.forEach { modelData ->
                val meshes = modelData.meshes.map { meshData ->

                    val (vertexSource, vertexDestination) = meshData.createVertexBuffers()
                    val (indexSource, indexDestination) = meshData.createIndexBuffers()

                    stagingBuffers.addAll(listOf(vertexSource, indexSource))

                    TransferBuffer(vertexSource, vertexDestination).record(cmd)
                    TransferBuffer(indexSource, indexDestination).record(cmd)

                    Mesh(meshData.id, vertexDestination, indexDestination, meshData.indices.size)
                }

                modelMap[modelData.id] = Model(modelData.id).apply {
                    meshList.addAll(meshes)
                }
            }
        }

        cmd.submitAndWait(queue)
        cmd.cleanup()

        stagingBuffers.forEach(Buffer::cleanup)
    }

    private fun MeshData.createIndexBuffers(): TransferBuffer {
        val bufferSize = (indices.size * Int.SIZE_BYTES).toLong()
        return createTransferBuffers(bufferSize, VK_BUFFER_USAGE_INDEX_BUFFER_BIT) { ptr ->
            MemoryUtil.memIntBuffer(ptr, indices.size).put(indices)
        }
    }

    private fun MeshData.createVertexBuffers(): TransferBuffer {
        val bufferSize = (positions.size * Float.SIZE_BYTES).toLong()
        return createTransferBuffers(bufferSize, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT) { ptr ->
            MemoryUtil.memFloatBuffer(ptr, positions.size).put(positions)
        }
    }

    private inline fun createTransferBuffers(
        bufferSize: Long,
        dstUsage: Int,
        populateData: (Long) -> Unit
    ): TransferBuffer {
        val srcBuffer = Buffer(
            requestedSize = bufferSize,
            usage = VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
            reqMask = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
        )
        val dstBuffer = Buffer(
            requestedSize = bufferSize,
            usage = VK_BUFFER_USAGE_TRANSFER_DST_BIT or dstUsage,
            reqMask = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
        )

        srcBuffer.map().also { ptr ->
            populateData(ptr)
            srcBuffer.unmap()
        }

        return TransferBuffer(srcBuffer, dstBuffer)
    }

    fun cleanup() {
        modelMap.values.forEach(Model::cleanup)
        modelMap.clear()
    }
}