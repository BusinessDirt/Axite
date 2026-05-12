package github.businessdirt.axite.vanadium.assets.loaders

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.assets.types.Shader
import github.businessdirt.axite.vanadium.assets.types.ShaderMetadata
import github.businessdirt.axite.vanadium.assets.types.ShaderStage
import github.businessdirt.axite.vanadium.vulkan.resources.ShaderModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.lwjgl.util.shaderc.Shaderc
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
class ShaderSerializer : AssetSerializer<Shader, ShaderMetadata> {

    private val json = Json { prettyPrint = true }

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
            
            val pCode = compileAndCache(glslFile, spvFile, stage, finalMetadata)
            metadata = finalMetadata
            pCode
        }

        val module = ShaderModule(Vanadium.context.device.handle, stage.vulkan, pCode)
        Shader(path, metadata.uuid, metadata, stage, module)
    }

    override fun loadMetadata(path: String): ShaderMetadata? {
        val file = File("$path.meta")
        return if (file.exists()) {
            try {
                json.decodeFromString<ShaderMetadata>(file.readText())
            } catch (e: Exception) {
                logger.warn("Failed to load metadata for [{}]: {}", path, e.message)
                null
            }
        } else null
    }

    override fun writeMetadata(path: String, metadata: ShaderMetadata) {
        try {
            File("$path.meta").writeText(json.encodeToString(metadata))
        } catch (e: Exception) {
            logger.warn("Failed to write metadata for [{}]: {}", path, e.message)
        }
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

    private fun compileAndCache(glslFile: File, spvFile: File, stage: ShaderStage, metadata: ShaderMetadata): ByteBuffer {
        logger.debug("Compiling shader: [{}]", glslFile.path)

        val shaderCode = glslFile.readText()
        val compiledBytes = compileShader(shaderCode, stage.shaderc, glslFile.name)

        // Cache to disk in the background using the engine scope
        Vanadium.engineScope.launch(Dispatchers.IO) {
            try {
                spvFile.writeBytes(compiledBytes)
                writeMetadata(glslFile.path, metadata)
                logger.debug("Cached compiled SPV and metadata to [{}]", spvFile.path)
            } catch (e: Exception) {
                logger.warn("Failed to cache SPV or metadata for [{}]: {}", glslFile.path, e.message)
            }
        }

        // Return a Direct ByteBuffer for Vulkan
        return ByteBuffer.allocateDirect(compiledBytes.size)
            .order(ByteOrder.nativeOrder())
            .put(compiledBytes)
            .flip()
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
