package github.businessdirt.axite.vanadium.assets.loaders

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.assets.AssetLoader
import github.businessdirt.axite.vanadium.assets.ShaderCompiler
import github.businessdirt.axite.vanadium.assets.types.Shader
import github.businessdirt.axite.vanadium.assets.types.ShaderStage
import github.businessdirt.axite.vanadium.vulkan.resources.ShaderModule

class ShaderLoader : AssetLoader<Shader> {

    override suspend fun load(path: String): Shader {
        val stage: ShaderStage = ShaderStage.fromPath(path)
        ShaderCompiler.compileShaderIfChanged(path, stage.shaderc)
        return Shader(path, stage, ShaderModule(Vanadium.context.device.handle, stage.vulkan, "$path.spv"))
    }
}