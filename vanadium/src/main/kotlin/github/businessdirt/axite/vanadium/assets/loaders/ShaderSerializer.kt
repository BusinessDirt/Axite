package github.businessdirt.axite.vanadium.assets.loaders

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.assets.metadata.LayoutBinding
import github.businessdirt.axite.vanadium.assets.metadata.PushConstantRange
import github.businessdirt.axite.vanadium.assets.metadata.ShaderMetadata
import github.businessdirt.axite.vanadium.assets.metadata.VertexInputAttribute
import github.businessdirt.axite.vanadium.assets.types.Shader
import github.businessdirt.axite.vanadium.assets.types.ShaderStage
import github.businessdirt.axite.vanadium.core.utils.getPointer
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.resources.ShaderModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.serializer
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.shaderc.Shaderc
import org.lwjgl.util.spvc.Spv.SpvDecorationBinding
import org.lwjgl.util.spvc.Spv.SpvDecorationLocation
import org.lwjgl.util.spvc.Spvc.*
import org.lwjgl.util.spvc.SpvcReflectedResource
import org.lwjgl.vulkan.VK13.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

/**
 * Loads and compiles shaders.
 * Supports caching compiled SPIR-V on disk.
 */
class ShaderSerializer : AssetSerializer<Shader, ShaderMetadata>(
    serializer<ShaderMetadata>()
) {

    override suspend fun load(path: String): Shader = withContext(Dispatchers.IO) {
        val stage = ShaderStage.fromPath(path)
        val glslFile = File(path)
        val spvFile = File("$path.spv")

        val currentHash = calculateHash(glslFile)
        var metadata = loadMetadata(path)

        val pCode = if (spvFile.exists() && metadata != null && metadata.hash == currentHash) {
            logger.debug("Loading cached SPV: [{}]", spvFile.path)
            loadCachedSpv(spvFile)
        } else {
            val finalMetadata = metadata?.copy(
                hash = currentHash,
                compilationTime = System.currentTimeMillis(),
                stage = stage
            ) ?: ShaderMetadata(
                hash = currentHash,
                stage = stage,
                compilationTime = System.currentTimeMillis()
            )
            
            val (pCode, reflectedMetadata) = compileAndCache(glslFile, spvFile, stage, finalMetadata)
            metadata = reflectedMetadata
            pCode
        }

        val module = ShaderModule(Vanadium.context.device.handle, stage.vulkan, pCode)
        Shader(path, metadata.uuid, metadata, stage, module)
    }

    private fun calculateHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read = input.read(buffer)
            while (read != -1) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun loadCachedSpv(spvFile: File): ByteBuffer {
        return FileChannel.open(spvFile.toPath(), StandardOpenOption.READ).use { channel ->
            channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
        }
    }

    private fun compileAndCache(glslFile: File, spvFile: File, stage: ShaderStage, metadata: ShaderMetadata): Pair<ByteBuffer, ShaderMetadata> {
        logger.debug("Compiling shader: [{}]", glslFile.path)

        val shaderCode = glslFile.readText()
        val compiledBytes = compileShader(shaderCode, stage.shaderc, glslFile.name)

        // Return a Direct ByteBuffer for Vulkan
        val pCode = ByteBuffer.allocateDirect(compiledBytes.size)
            .order(ByteOrder.nativeOrder())
            .put(compiledBytes)
            .flip()

        // Reflect shader
        val reflectedMetadata = reflectShader(pCode, metadata)

        // Cache to disk in the background using the engine scope
        Vanadium.engineScope.launch(Dispatchers.IO) {
            try {
                spvFile.writeBytes(compiledBytes)
                writeMetadata(glslFile.path, reflectedMetadata)
                logger.debug("Cached compiled SPV and metadata to [{}]", spvFile.path)
            } catch (e: Exception) {
                logger.warn("Failed to cache SPV or metadata for [{}]: {}", glslFile.path, e.message)
            }
        }

        return pCode to reflectedMetadata
    }

    private fun reflectShader(pCode: ByteBuffer, metadata: ShaderMetadata): ShaderMetadata = memoryStack { stack ->
        val context = stack.getPointer(::spvc_context_create)

        try {
            val ir = stack.getPointer { irPtr ->
                val spvData = pCode.asIntBuffer()
                spvc_context_parse_spirv(context, spvData, spvData.remaining().toLong(), irPtr)
            }

            val compiler = stack.getPointer { compilerPtr ->
                spvc_context_create_compiler(context, SPVC_BACKEND_NONE, ir, SPVC_CAPTURE_MODE_TAKE_OWNERSHIP, compilerPtr)
            }

            val resources = stack.getPointer { spvc_compiler_create_shader_resources(compiler, it) }

            val layoutBindings = mutableListOf<LayoutBinding>()
            val pushConstants = mutableListOf<PushConstantRange>()
            val vertexAttributes = mutableListOf<VertexInputAttribute>()

            // Reflect Uniform Buffers
            reflectResourceType(compiler, resources, SPVC_RESOURCE_TYPE_UNIFORM_BUFFER, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, metadata.stage.vulkan, layoutBindings)
            // Reflect Storage Buffers
            reflectResourceType(compiler, resources, SPVC_RESOURCE_TYPE_STORAGE_BUFFER, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, metadata.stage.vulkan, layoutBindings)
            // Reflect Sampled Images (Combined Image Sampler)
            reflectResourceType(compiler, resources, SPVC_RESOURCE_TYPE_SAMPLED_IMAGE, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, metadata.stage.vulkan, layoutBindings)
            // Reflect Storage Images
            reflectResourceType(compiler, resources, SPVC_RESOURCE_TYPE_STORAGE_IMAGE, VK_DESCRIPTOR_TYPE_STORAGE_IMAGE, metadata.stage.vulkan, layoutBindings)

            // Reflect Push Constants
            val pushConstantListPtr = stack.mallocPointer(1)
            val pushConstantCount = stack.getPointer { pushConstantCountPtr ->
                spvc_resources_get_resource_list_for_type(resources, SPVC_RESOURCE_TYPE_PUSH_CONSTANT, pushConstantListPtr, pushConstantCountPtr)
            }.toInt()
            val pcList = SpvcReflectedResource.create(pushConstantListPtr[0], pushConstantCount)

            for (i in 0 until pushConstantCount) {
                val resource = pcList[i]
                val type = spvc_compiler_get_type_handle(compiler, resource.type_id())
                val size = stack.getPointer { sizePtr -> spvc_compiler_get_declared_struct_size(compiler, type, sizePtr) }

                pushConstants.add(PushConstantRange(metadata.stage.vulkan, 0, size.toInt()))
            }

            // Reflect Vertex Inputs (only for vertex stage)
            if (metadata.stage == ShaderStage.VERTEX) {
                val inputListPtr = stack.mallocPointer(1)
                val inputCount = stack.getPointer { inputCountPtr ->
                    spvc_resources_get_resource_list_for_type(resources, SPVC_RESOURCE_TYPE_STAGE_INPUT, inputListPtr, inputCountPtr)
                }.toInt()
                val inputList = SpvcReflectedResource.create(inputListPtr[0], inputCount)

                for (i in 0 until inputCount) {
                    val resource = inputList[i]
                    val location = spvc_compiler_get_decoration(compiler, resource.id(), SpvDecorationLocation)
                    val format = getVulkanFormat(compiler, resource.type_id())
                    val binding = if (spvc_compiler_has_decoration(compiler, resource.id(), SpvDecorationBinding)) {
                        spvc_compiler_get_decoration(compiler, resource.id(), SpvDecorationBinding)
                    } else 0

                    vertexAttributes.add(VertexInputAttribute(location, binding, format, 0))
                }
            }

            metadata.copy(
                pushConstantRanges = pushConstants,
                layoutBindings = layoutBindings,
                vertexInputAttributes = vertexAttributes
            )
        } catch (e: Exception) {
            logger.error("Failed to reflect shader [{}]: {}", metadata.hash, e.message ?: "Unknown error")
            metadata
        } finally {
            spvc_context_destroy(context)
        }
    }

    private fun reflectResourceType(
        compiler: Long,
        resources: Long,
        spvcType: Int,
        vkType: Int,
        stageFlags: Int,
        results: MutableList<LayoutBinding>
    ) {
        MemoryStack.stackPush().use { stack ->
            val listPtr = stack.mallocPointer(1)
            val count = stack.getPointer { countPtr ->
                spvc_resources_get_resource_list_for_type(resources, spvcType, listPtr, countPtr)
            }.toInt()

            if (count == 0) return

            val list = SpvcReflectedResource.create(listPtr[0], count)

            for (i in 0 until count) {
                val resource = list[i]
                val binding = spvc_compiler_get_decoration(compiler, resource.id(), SpvDecorationBinding)
                val name = spvc_compiler_get_name(compiler, resource.id()) ?: ""

                results.add(LayoutBinding(binding, vkType, 1, stageFlags, name))
            }
        }
    }

    private fun getVulkanFormat(compiler: Long, typeId: Int): Int {
        val type = spvc_compiler_get_type_handle(compiler, typeId)
        val baseType = spvc_type_get_basetype(type)
        val vecSize = spvc_type_get_vector_size(type)
        
        return when (baseType) {
            SPVC_BASETYPE_FP32 -> when (vecSize) {
                1 -> VK_FORMAT_R32_SFLOAT
                2 -> VK_FORMAT_R32G32_SFLOAT
                3 -> VK_FORMAT_R32G32B32_SFLOAT
                4 -> VK_FORMAT_R32G32B32A32_SFLOAT
                else -> VK_FORMAT_UNDEFINED
            }
            SPVC_BASETYPE_INT32 -> when (vecSize) {
                1 -> VK_FORMAT_R32_SINT
                2 -> VK_FORMAT_R32G32_SINT
                3 -> VK_FORMAT_R32G32B32_SINT
                4 -> VK_FORMAT_R32G32B32A32_SINT
                else -> VK_FORMAT_UNDEFINED
            }
            SPVC_BASETYPE_UINT32 -> when (vecSize) {
                1 -> VK_FORMAT_R32_UINT
                2 -> VK_FORMAT_R32G32_UINT
                3 -> VK_FORMAT_R32G32B32_UINT
                4 -> VK_FORMAT_R32G32B32A32_UINT
                else -> VK_FORMAT_UNDEFINED
            }
            else -> VK_FORMAT_UNDEFINED
        }
    }

    /**
     * Compiles GLSL source code into SPIR-V bytes using Shaderc.
     */
    private fun compileShader(shaderCode: String, shaderType: Int, fileName: String = "shader.glsl"): ByteArray {
        val compiler = Shaderc.shaderc_compiler_initialize()
        val options = Shaderc.shaderc_compile_options_initialize()

        try {
            Shaderc.shaderc_compile_options_set_generate_debug_info(options)
            Shaderc.shaderc_compile_options_set_optimization_level(options, Shaderc.shaderc_optimization_level_zero)
            Shaderc.shaderc_compile_options_set_source_language(options, Shaderc.shaderc_source_language_glsl)

            val result = Shaderc.shaderc_compile_into_spv(
                compiler, shaderCode, shaderType,
                fileName, "main", options
            )

            if (result == 0L) throw RuntimeException("Shaderc returned null result pointer for $fileName")

            try {
                if (Shaderc.shaderc_result_get_compilation_status(result) != Shaderc.shaderc_compilation_status_success) {
                    val errorMessage = Shaderc.shaderc_result_get_error_message(result)
                    throw RuntimeException("Shader compilation failed for $fileName: $errorMessage")
                }

                val buffer = Shaderc.shaderc_result_get_bytes(result)
                    ?: throw RuntimeException("Shaderc returned null buffer for $fileName")

                return ByteArray(buffer.remaining()).also { buffer.get(it) }
            } finally {
                Shaderc.shaderc_result_release(result)
            }
        } finally {
            Shaderc.shaderc_compile_options_release(options)
            Shaderc.shaderc_compiler_release(compiler)
        }
    }
}
