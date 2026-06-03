package github.businessdirt.axite.vanadium.assets.metadata

import github.businessdirt.axite.vanadium.assets.types.ShaderStage
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class VertexInputBinding(
    val binding: Int,
    val stride: Int,
    val inputRate: Int
)

@Serializable
data class VertexInputAttribute(
    val location: Int,
    val binding: Int,
    val format: Int,
    val offset: Int
)

@Serializable
data class PushConstantRange(
    val stageFlags: Int,
    val offset: Int,
    val size: Int
)

@Serializable
data class LayoutBinding(
    val set: Int = 0,
    val binding: Int,
    val descriptorType: Int,
    val descriptorCount: Int,
    val stageFlags: Int,
    val name: String = ""
)

@Serializable
data class SpecializationConstant(
    val id: Int,
    val constantId: Int,
    val name: String,
    val type: Int // SpvcBasetype
)

@Serializable
data class ShaderMetadata(
    override val uuid: String = UUID.randomUUID().toString(),
    override val version: Int = 1,
    val hash: String = "",
    val stage: ShaderStage,
    val compilationTime: Long = 0L,
    val definitions: Map<String, String> = emptyMap(),
    val vertexInputBindings: List<VertexInputBinding> = emptyList(),
    val vertexInputAttributes: List<VertexInputAttribute> = emptyList(),
    val pushConstantRanges: List<PushConstantRange> = emptyList(),
    val layoutBindings: List<LayoutBinding> = emptyList(),
    val specializationConstants: List<SpecializationConstant> = emptyList()
) : AssetMetadata()
