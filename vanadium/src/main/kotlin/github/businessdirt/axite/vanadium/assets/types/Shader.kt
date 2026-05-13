package github.businessdirt.axite.vanadium.assets.types

import github.businessdirt.axite.vanadium.assets.metadata.ShaderMetadata
import github.businessdirt.axite.vanadium.vulkan.resources.ShaderModule
import org.lwjgl.util.shaderc.Shaderc
import org.lwjgl.vulkan.KHRRayTracingPipeline.*
import org.lwjgl.vulkan.VK13.*

class Shader(
    path: String,
    uuid: String,
    metadata: ShaderMetadata,
    stage: ShaderStage,
    module: ShaderModule
) : Asset<Shader>(uuid, path, metadata) {

    override var metadata: ShaderMetadata = metadata
        private set

    var stage: ShaderStage = stage
        private set

    var module: ShaderModule = module
        private set

    override fun update(newAsset: Shader) {
        val oldModule = this.module
        this.metadata = newAsset.metadata
        this.stage = newAsset.stage
        this.module = newAsset.module
        oldModule.close()
    }

    override fun dispose() = module.close()
}

enum class ShaderStage(
    val shaderc: Int,
    val vulkan: Int,
    val extensions: List<String>
) {
    VERTEX(
        Shaderc.shaderc_vertex_shader,
        VK_SHADER_STAGE_VERTEX_BIT,
        listOf("vert", "vertex")
    ),
    FRAGMENT(
        Shaderc.shaderc_fragment_shader,
        VK_SHADER_STAGE_FRAGMENT_BIT,
        listOf("frag", "fragment")
    ),
    COMPUTE(
        Shaderc.shaderc_compute_shader,
        VK_SHADER_STAGE_COMPUTE_BIT,
        listOf("comp", "compute")
    ),
    GEOMETRY(
        Shaderc.shaderc_geometry_shader,
        VK_SHADER_STAGE_GEOMETRY_BIT,
        listOf("geom", "geometry")
    ),
    TESSELLATION_CONTROL(
        Shaderc.shaderc_tess_control_shader,
        VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT,
        listOf("tesc")
    ),
    TESSELLATION_EVALUATION(
        Shaderc.shaderc_tess_evaluation_shader,
        VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT,
        listOf("tese")
    ),
    // Ray Tracing Stages
    RAYGEN(
        Shaderc.shaderc_raygen_shader,
        VK_SHADER_STAGE_RAYGEN_BIT_KHR,
        listOf("rgen")
    ),
    MISS(
        Shaderc.shaderc_miss_shader,
        VK_SHADER_STAGE_MISS_BIT_KHR,
        listOf("rmiss")
    ),
    CLOSEST_HIT(
        Shaderc.shaderc_closesthit_shader,
        VK_SHADER_STAGE_CLOSEST_HIT_BIT_KHR,
        listOf("rchit")
    );

    companion object {

        /**
         * Finds the stage based on the file path.
         * Handles formats like: "tri.vert", "tri.vert.glsl", "tri.vertex"
         */
        fun fromPath(path: String): ShaderStage {
            val fileName = path.lowercase()

            // We search for the stage identifiers within the filename segments
            // This handles both .vert and .vert.glsl
            return entries.find { stage ->
                stage.extensions.any { ext ->
                    fileName.contains(".$ext.") || fileName.endsWith(".$ext")
                }
            } ?: throw IllegalArgumentException("Could not determine shader stage from path: $path")
        }
    }
}
