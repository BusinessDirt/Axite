package github.businessdirt.axite.vanadium.assets.loaders

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.assets.metadata.*
import github.businessdirt.axite.vanadium.assets.types.Shader
import github.businessdirt.axite.vanadium.assets.types.ShaderStage
import github.businessdirt.axite.vanadium.core.utils.getPointer
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.resources.ShaderModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.shaderc.Shaderc
import org.lwjgl.util.spvc.Spv.*
import org.lwjgl.util.spvc.Spvc.*
import org.lwjgl.util.spvc.SpvcReflectedResource
import org.lwjgl.util.spvc.SpvcSpecializationConstant
import org.lwjgl.vulkan.VK13.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/**
 * Loads and compiles shaders.
 * Supports caching compiled SPIR-V on disk.
 */
class ShaderSerializer : AssetSerializer<Shader, ShaderMetadata>(
    ShaderMetadata.serializer()
) {

    override suspend fun load(path: String): Shader = withContext(Dispatchers.IO) {
        val stage = ShaderStage.fromPath(path)
        val glslFile = File(path)
        val spvFile = File("$path.spv")

        val currentHash = glslFile.calculateHash()
        var metadata = loadMetadata(path)

        val pCode = if (spvFile.exists() && metadata != null && metadata.hash == currentHash) {
            logger.debug("Loading cached SPV: [{}]", spvFile.path)
            spvFile.loadCachedSpv()
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

            val (compiledCode, reflectedMetadata) = compileAndCache(glslFile, spvFile, stage, finalMetadata)
            metadata = reflectedMetadata
            compiledCode
        }

        val module = ShaderModule(Vanadium.context.device.handle, stage.vulkan, pCode)
        Shader(path, metadata.uuid, metadata, stage, module)
    }

    private fun compileAndCache(
        glslFile: File,
        spvFile: File,
        stage: ShaderStage,
        metadata: ShaderMetadata
    ): Pair<ByteBuffer, ShaderMetadata> {
        logger.debug("Compiling shader: [{}]", glslFile.path)

        val shaderCode = glslFile.readText()
        val compiledBytes = compileShader(shaderCode, stage.shaderc, glslFile.name)

        val pCode = MemoryUtil.memAlloc(compiledBytes.size)
            .order(ByteOrder.nativeOrder())
            .put(compiledBytes)
            .flip() as ByteBuffer

        val reflectedMetadata = reflectShader(pCode, metadata)

        // Offload IO operations asynchronously to engine scope safely
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

            if (compiler == 0L) throw RuntimeException("Failed to create SPVC compiler")

            val resources = stack.getPointer { spvc_compiler_create_shader_resources(compiler, it) }

            val layoutBindings = mutableListOf<LayoutBinding>()
            val pushConstants = mutableListOf<PushConstantRange>()
            val vertexAttributes = mutableListOf<VertexInputAttribute>()
            val vertexInputBindings = mutableListOf<VertexInputBinding>()
            val specializationConstants = mutableListOf<SpecializationConstant>()

            // Bulk gather descriptor sets layout properties
            reflectDescriptors(compiler, resources, metadata.stage.vulkan, layoutBindings)

            // Reflect Push Constants
            reflectPushConstants(compiler, resources, metadata.stage.vulkan, pushConstants)

            // Reflect Vertex Input Layout properties
            if (metadata.stage == ShaderStage.VERTEX) {
                reflectVertexInput(compiler, resources, vertexAttributes, vertexInputBindings)
            }

            // Reflect Specialization Constants with extra safety and full trace
            try {
                reflectSpecializationConstants(compiler, specializationConstants)
            } catch (e: Throwable) {
                logger.warn("Specialization constant reflection failed for [{}]:", metadata.hash, e)
            }

            metadata.copy(
                pushConstantRanges = pushConstants,
                layoutBindings = layoutBindings,
                vertexInputAttributes = vertexAttributes,
                vertexInputBindings = vertexInputBindings,
                specializationConstants = specializationConstants
            )
        } catch (e: Exception) {
            logger.error("Failed to reflect shader structural attributes [{}]: {}", metadata.hash, e.message ?: "Unknown native runtime error")
            metadata
        } finally {
            spvc_context_destroy(context)
        }
    }

    private fun reflectDescriptors(compiler: Long, resources: Long, stageFlags: Int, results: MutableList<LayoutBinding>) {
        reflectResourceType(compiler, resources, SPVC_RESOURCE_TYPE_UNIFORM_BUFFER, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, stageFlags, results)
        reflectResourceType(compiler, resources, SPVC_RESOURCE_TYPE_STORAGE_BUFFER, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, stageFlags, results)
        reflectResourceType(compiler, resources, SPVC_RESOURCE_TYPE_SAMPLED_IMAGE, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, stageFlags, results)
        reflectResourceType(compiler, resources, SPVC_RESOURCE_TYPE_STORAGE_IMAGE, VK_DESCRIPTOR_TYPE_STORAGE_IMAGE, stageFlags, results)
    }

    private fun reflectPushConstants(compiler: Long, resources: Long, stageFlags: Int, results: MutableList<PushConstantRange>) = memoryStack { stack ->
        val listPtr = stack.callocPointer(1)
        val countPtr = stack.callocPointer(1)
        val res = spvc_resources_get_resource_list_for_type(resources, SPVC_RESOURCE_TYPE_PUSH_CONSTANT, listPtr, countPtr)
        
        if (res == SPVC_SUCCESS) {
            val count = countPtr[0].toInt()
            if (count > 0) {
                val pcList = SpvcReflectedResource.create(listPtr[0], count)
                for (i in 0 until count) {
                    val resource = pcList[i]
                    val type = spvc_compiler_get_type_handle(compiler, resource.type_id())
                    val sizePtr = stack.callocPointer(1)
                    if (spvc_compiler_get_declared_struct_size(compiler, type, sizePtr) == SPVC_SUCCESS) {
                        results.add(PushConstantRange(stageFlags, 0, sizePtr[0].toInt()))
                    }
                }
            }
        }
    }

    private fun reflectVertexInput(
        compiler: Long,
        resources: Long,
        attributes: MutableList<VertexInputAttribute>,
        bindings: MutableList<VertexInputBinding>
    ) = memoryStack { stack ->
        val inputListPtr = stack.callocPointer(1)
        val countPtr = stack.callocPointer(1)
        val res = spvc_resources_get_resource_list_for_type(resources, SPVC_RESOURCE_TYPE_STAGE_INPUT, inputListPtr, countPtr)

        if (res == SPVC_SUCCESS) {
            val inputCount = countPtr[0].toInt()
            if (inputCount > 0) {
                val inputList = SpvcReflectedResource.create(inputListPtr[0], inputCount)
                val reflectedInputs = ArrayList<VertexAttributeTemp>(inputCount)

                for (i in 0 until inputCount) {
                    val resource = inputList[i]
                    val location = spvc_compiler_get_decoration(compiler, resource.id(), SpvDecorationLocation)
                    val format = getVulkanFormat(compiler, resource.type_id())
                    val binding = if (spvc_compiler_has_decoration(compiler, resource.id(), SpvDecorationBinding)) {
                        spvc_compiler_get_decoration(compiler, resource.id(), SpvDecorationBinding)
                    } else 0
                    reflectedInputs.add(VertexAttributeTemp(location, format, binding))
                }

                reflectedInputs.sortBy { it.location }

                var strideOffset = 0
                for (attr in reflectedInputs) {
                    attributes.add(VertexInputAttribute(attr.location, attr.binding, attr.format, strideOffset))
                    strideOffset += getFormatSize(attr.format)
                }

                if (attributes.isNotEmpty()) {
                    bindings.add(VertexInputBinding(0, strideOffset, VK_VERTEX_INPUT_RATE_VERTEX))
                }
            }
        }
    }

    private fun reflectSpecializationConstants(compiler: Long, results: MutableList<SpecializationConstant>) = memoryStack { stack ->
        val constantsPtr = stack.callocPointer(1)
        val numConstantsPtr = stack.callocPointer(1)

        val result = spvc_compiler_get_specialization_constants(compiler, constantsPtr, numConstantsPtr)
        if (result != SPVC_SUCCESS) {
            logger.warn("spvc_compiler_get_specialization_constants failed with code: {}", result)
            return@memoryStack
        }

        val numConstants = numConstantsPtr.get(0).toInt()
        if (numConstants <= 0) return@memoryStack

        val constantsAddr = constantsPtr.get(0)
        if (constantsAddr == 0L) return@memoryStack

        val constants = SpvcSpecializationConstant.create(constantsAddr, numConstants)
        for (i in 0 until numConstants) {
            val constant = constants.get(i)
            val id = constant.id()
            val constantId = constant.constant_id()
            val name = spvc_compiler_get_name(compiler, id)?.takeIf { it.isNotEmpty() } ?: "spec_const_$constantId"
            val constantHandle = spvc_compiler_get_constant_handle(compiler, id)

            val type = if (constantHandle != 0L) {
                val typeId = spvc_constant_get_type(constantHandle)
                if (typeId != 0) {
                    val nativeTypeHandle = spvc_compiler_get_type_handle(compiler, typeId)
                    if (nativeTypeHandle != 0L) spvc_type_get_basetype(nativeTypeHandle) else {
                        logger.warn("Could not resolve native type handle for type ID: $typeId")
                        SPVC_BASETYPE_INT32
                    }
                } else SPVC_BASETYPE_INT32
            } else SPVC_BASETYPE_INT32

            logger.debug("Reflected Spec Constant: [name: {}, id: {}, constant_id: {}, type: {}]", name, id, constantId, type)
            results.add(SpecializationConstant(id, constantId, name, type))
        }
    }

    private fun reflectResourceType(
        compiler: Long,
        resources: Long,
        spvcType: Int,
        vkType: Int,
        stageFlags: Int,
        results: MutableList<LayoutBinding>
    ) = memoryStack { stack ->
        val listPtr = stack.callocPointer(1)
        val countPtr = stack.callocPointer(1)
        val res = spvc_resources_get_resource_list_for_type(resources, spvcType, listPtr, countPtr)

        if (res != SPVC_SUCCESS) return@memoryStack

        val count = countPtr[0].toInt()
        if (count == 0) return@memoryStack

        val list = SpvcReflectedResource.create(listPtr[0], count)
        for (i in 0 until count) {
            val resource = list[i]
            val set = spvc_compiler_get_decoration(compiler, resource.id(), SpvDecorationDescriptorSet)
            val binding = spvc_compiler_get_decoration(compiler, resource.id(), SpvDecorationBinding)
            val name = spvc_compiler_get_name(compiler, resource.id()) ?: ""

            val type = spvc_compiler_get_type_handle(compiler, resource.type_id())
            val arraySize = if (spvc_type_get_num_array_dimensions(type) > 0) {
                spvc_type_get_array_dimension(type, 0)
            } else 1

            results.add(LayoutBinding(set, binding, vkType, arraySize, stageFlags, name))
        }
    }

    private fun getFormatSize(format: Int): Int = when (format) {
        VK_FORMAT_R32_SFLOAT, VK_FORMAT_R32_SINT, VK_FORMAT_R32_UINT -> 4
        VK_FORMAT_R32G32_SFLOAT, VK_FORMAT_R32G32_SINT, VK_FORMAT_R32G32_UINT -> 8
        VK_FORMAT_R32G32B32_SFLOAT, VK_FORMAT_R32G32B32_SINT, VK_FORMAT_R32G32B32_UINT -> 12
        VK_FORMAT_R32G32B32A32_SFLOAT, VK_FORMAT_R32G32B32A32_SINT, VK_FORMAT_R32G32B32A32_UINT -> 16
        else -> 0
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

    private fun compileShader(shaderCode: String, shaderType: Int, fileName: String = "shader.glsl"): ByteArray {
        val compiler = Shaderc.shaderc_compiler_initialize()
        if (compiler == 0L) throw RuntimeException("Failed to initialize Shaderc context compiler")

        val options = Shaderc.shaderc_compile_options_initialize()
        if (options == 0L) {
            Shaderc.shaderc_compiler_release(compiler)
            throw RuntimeException("Failed to initialize Shaderc compiler build options configuration")
        }

        try {
            Shaderc.shaderc_compile_options_set_generate_debug_info(options)
            Shaderc.shaderc_compile_options_set_optimization_level(options, Shaderc.shaderc_optimization_level_zero)
            Shaderc.shaderc_compile_options_set_source_language(options, Shaderc.shaderc_source_language_glsl)

            val result = Shaderc.shaderc_compile_into_spv(compiler, shaderCode, shaderType, fileName, "main", options)
            if (result == 0L) throw RuntimeException("Shaderc returned an invalid structural result pointer address for: $fileName")

            try {
                if (Shaderc.shaderc_result_get_compilation_status(result) != Shaderc.shaderc_compilation_status_success) {
                    val errorMessage = Shaderc.shaderc_result_get_error_message(result)
                    throw RuntimeException("Shader compilation failed for $fileName: $errorMessage")
                }

                val buffer = Shaderc.shaderc_result_get_bytes(result)
                    ?: throw RuntimeException("Shaderc compilation data buffer returned null pointer for: $fileName")

                return ByteArray(buffer.remaining()).also { buffer.get(it) }
            } finally {
                Shaderc.shaderc_result_release(result)
            }
        } finally {
            Shaderc.shaderc_compile_options_release(options)
            Shaderc.shaderc_compiler_release(compiler)
        }
    }

    private data class VertexAttributeTemp(val location: Int, val format: Int, val binding: Int)
}

private fun File.calculateHash(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(8192)

    this.inputStream().use { input ->
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
    }

    return digest.digest().joinToString("") { "%02x".format(it) }
}

private fun File.loadCachedSpv(): ByteBuffer {
    val bytes = this.readBytes()
    val buffer = MemoryUtil.memAlloc(bytes.size)
        .order(ByteOrder.nativeOrder())
        .put(bytes)
    return buffer.flip() as ByteBuffer
}
