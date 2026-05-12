package github.businessdirt.axite.vanadium.assets

import github.businessdirt.axite.logging.LoggingConfigurator
import org.apache.logging.log4j.LogManager
import org.lwjgl.util.shaderc.Shaderc
import java.io.File

object ShaderCompiler {

    private val logger = LogManager.getLogger(this::class.java)

    fun compileShader(shaderCode: String, shaderType: Int, fileName: String = "shader.glsl"): ByteArray {
        val compiler = Shaderc.shaderc_compiler_initialize()
        val options = Shaderc.shaderc_compile_options_initialize()
        var result = 0L

        try {
            if (LoggingConfigurator.isDebugMode) {
                Shaderc.shaderc_compile_options_set_generate_debug_info(options)
                Shaderc.shaderc_compile_options_set_optimization_level(options, 0)
                Shaderc.shaderc_compile_options_set_source_language(options, Shaderc.shaderc_source_language_glsl)
            }

            result = Shaderc.shaderc_compile_into_spv(
                compiler,
                shaderCode,
                shaderType,
                fileName,
                "main",
                options
            )

            if (Shaderc.shaderc_result_get_compilation_status(result) != Shaderc.shaderc_compilation_status_success) {
                val errorMessage = Shaderc.shaderc_result_get_error_message(result)
                throw RuntimeException("Shader compilation failed for $fileName: $errorMessage")
            }

            // Get bytes from the C buffer
            val buffer = Shaderc.shaderc_result_get_bytes(result)
                ?: throw RuntimeException("Shaderc returned null buffer")

            val compiledShader = ByteArray(buffer.remaining())
            buffer.get(compiledShader)

            return compiledShader

        } finally {
            if (result != 0L) Shaderc.shaderc_result_release(result)
            Shaderc.shaderc_compile_options_release(options)
            Shaderc.shaderc_compiler_release(compiler)
        }
    }

    fun compileShaderIfChanged(glslShaderPath: String, shaderType: Int) {
        val glslFile = File(glslShaderPath)
        val spvFile = File("$glslShaderPath.spv")

        if (!spvFile.exists() || glslFile.lastModified() > spvFile.lastModified()) {
            logger.debug("Compiling [{}] to [{}]", glslFile.path, spvFile.path)

            // Kotlin I/O extensions make this a one-liner
            val shaderCode = glslFile.readText()

            val compiledShader = compileShader(shaderCode, shaderType, glslFile.name)

            // And writing is a one-liner too
            spvFile.writeBytes(compiledShader)
        } else {
            logger.debug("Shader [{}] already compiled. Loading compiled version: [{}]", glslFile.path, spvFile.path)
        }
    }
}