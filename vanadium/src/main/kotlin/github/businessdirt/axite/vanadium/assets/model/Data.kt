package github.businessdirt.axite.vanadium.assets.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.joml.Vector4f
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.DataOutputStream
import java.io.FileOutputStream

@Serializable
@SerialName("Vector4f")
private class Vector4fSurrogate(val x: Float, val y: Float, val z: Float, val w: Float)

object Vector4fSerializer : KSerializer<Vector4f> {
    override val descriptor: SerialDescriptor = Vector4fSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Vector4f) {
        val surrogate = Vector4fSurrogate(value.x, value.y, value.z, value.w)
        encoder.encodeSerializableValue(Vector4fSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Vector4f {
        val surrogate = decoder.decodeSerializableValue(Vector4fSurrogate.serializer())
        return Vector4f(surrogate.x, surrogate.y, surrogate.z, surrogate.w)
    }
}

@Serializable
data class ModelData(
    val id: String,
    val meshes: List<MeshData> = emptyList(),
    val vertexFilePath: String? = null,
    val indexFilePath: String? = null
)

@Serializable
data class MaterialData(
    val id: String,
    val texturePath: String? = null,
    @Serializable(with = Vector4fSerializer::class)
    val diffuseColor: Vector4f = Vector4f(1f, 1f, 1f, 1f)
)

@Serializable
data class MeshData(
    val id: String,
    val materialId: String,
    val vertexOffset: Int,
    val vertexSize: Int,
    val indexOffset: Int,
    val indexSize: Int
)

class ModelBinData(modelPath: String) : Closeable {

    private val basePath = modelPath.substringBeforeLast('.')

    val vertexFilePath: String = "$basePath.vertex"
    val indexFilePath: String = "$basePath.index"

    val vertexOutput = DataOutputStream(BufferedOutputStream(FileOutputStream(vertexFilePath)))
    val indexOutput = DataOutputStream(BufferedOutputStream(FileOutputStream(indexFilePath)))

    var vertexOffset: Int = 0
        private set

    var indexOffset: Int = 0
        private set

    fun incrementVertexOffset(increment: Int = 1) {
        vertexOffset += increment
    }

    fun incrementIndexOffset(increment: Int = 1) {
        indexOffset += increment
    }

    override fun close() {
        vertexOutput.close()
        indexOutput.close()
    }
}