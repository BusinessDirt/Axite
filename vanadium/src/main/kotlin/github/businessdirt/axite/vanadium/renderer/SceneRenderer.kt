package github.businessdirt.axite.vanadium.renderer

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.assets.types.Shader
import github.businessdirt.axite.vanadium.assets.types.Texture
import github.businessdirt.axite.vanadium.core.profiling.Profiler
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.scene.Mesh
import github.businessdirt.axite.vanadium.scene.Scene
import github.businessdirt.axite.vanadium.scene.components.*
import github.businessdirt.axite.vanadium.vulkan.Context
import github.businessdirt.axite.vanadium.vulkan.commands.*
import github.businessdirt.axite.vanadium.vulkan.descriptors.DescriptorPool
import github.businessdirt.axite.vanadium.vulkan.descriptors.DescriptorSet
import github.businessdirt.axite.vanadium.vulkan.pipeline.GraphicsPipeline
import github.businessdirt.axite.vanadium.vulkan.resources.Buffer
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK13.*

class SceneRenderer(val context: Context) {

    companion object {
        const val MAX_TEXTURES = 128
        const val MAX_MATERIALS = 1024

        const val VERTEX_SHADER_PATH = "src/main/resources/shaders/scene.vert.glsl"
        const val FRAGMENT_SHADER_PATH = "src/main/resources/shaders/scene.frag.glsl"
        const val WHITE_TEXTURE_PATH = "src/main/resources/textures/white.png"
    }

    private var graphicsPipeline: GraphicsPipeline? = null
    private var descriptorPool: DescriptorPool? = null
    
    private var projSets: Array<DescriptorSet>? = null
    private var viewSets: Array<DescriptorSet>? = null
    private var materialSets: Array<DescriptorSet>? = null
    private var textureSets: Array<DescriptorSet>? = null

    private var projBuffers: Array<Buffer>? = null
    private var viewBuffers: Array<Buffer>? = null
    private var materialBuffers: Array<Buffer>? = null

    private val textures = mutableListOf<Texture>()
    private lateinit var whiteTexture: Texture
    private var lastTextureHandles: LongArray? = null

    suspend fun initialize() = Profiler.profile("SceneRenderer Initialization") {
        val vertexShader = Vanadium.assets.load<Shader>(VERTEX_SHADER_PATH)
        val fragmentShader = Vanadium.assets.load<Shader>(FRAGMENT_SHADER_PATH)
        whiteTexture = Vanadium.assets.load<Texture>(WHITE_TEXTURE_PATH)

        // Ensure white texture is always at index 0
        textures.clear()
        textures.add(whiteTexture)

        graphicsPipeline = GraphicsPipeline {
            vertexShader(vertexShader)
            fragmentShader(fragmentShader)
            this.colorFormat = Vanadium.context.surface.surfaceFormat.imageFormat
            enableBlend = true
        }

        val frames = context.maxFramesInFlight
        descriptorPool = DescriptorPool(
            context.device.handle, frames * 4, listOf(
                DescriptorPool.PoolSize(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, frames * 2),
                DescriptorPool.PoolSize(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, frames),
                DescriptorPool.PoolSize(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, frames * MAX_TEXTURES)
            )
        )

        projBuffers = Array(frames) { Buffer(context.device.handle, context.physicalDevice, 64, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT) }
        viewBuffers = Array(frames) { Buffer(context.device.handle, context.physicalDevice, 64, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT) }

        val materialBufferSize = MAX_MATERIALS * 32L
        materialBuffers = Array(frames) { Buffer(context.device.handle, context.physicalDevice, materialBufferSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT) }

        graphicsPipeline?.layout?.descriptorSetLayouts?.let { layouts ->
            projSets = Array(frames) { i ->
                DescriptorSet(context.device.handle, descriptorPool!!, layouts[0]).also { set ->
                    set.updateBuffer(0, projBuffers!![i].handle, 64)
                }
            }

            viewSets = Array(frames) { i ->
                DescriptorSet(context.device.handle, descriptorPool!!, layouts[1]).also { set ->
                    set.updateBuffer(0, viewBuffers!![i].handle, 64)
                }
            }

            materialSets = Array(frames) { i ->
                DescriptorSet(context.device.handle, descriptorPool!!, layouts[2]).also { set ->
                    set.updateBuffer(0, materialBuffers!![i].handle, materialBufferSize, type = VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                }
            }

            textureSets = Array(frames) { DescriptorSet(context.device.handle, descriptorPool!!, layouts[3]) }
            lastTextureHandles = LongArray(frames) { 0L }
            
            for (i in 0 until frames) {
                updateTextureSet(i)
            }
        }
    }

    private fun updateTextureSet(frameIndex: Int) {
        val views = LongArray(MAX_TEXTURES) { whiteTexture.view.handle }
        val samplers = LongArray(MAX_TEXTURES) { whiteTexture.sampler.handle }
        textures.take(MAX_TEXTURES).forEachIndexed { i, tex ->
            views[i] = tex.view.handle
            samplers[i] = tex.sampler.handle
        }
        textureSets!![frameIndex].updateImages(0, views, samplers)
        
        // Track the "hash" of textures in this set to avoid redundant updates
        lastTextureHandles!![frameIndex] = textures.fold(0L) { acc, tex -> acc xor tex.view.handle }
    }

    fun render(commandBuffer: CommandBuffer, scene: Scene) {
        val pipeline = graphicsPipeline ?: return
        val frameIndex = context.currentFrameIndex

        // Update texture list from scene
        var textureUpdateNeeded = false
        scene.forEachModel { _: TransformComponent, modelComp: ModelComponent ->
            modelComp.model?.let { model ->
                model.materials.forEach { mat ->
                    mat.albedoTexture?.let {
                        if (it !in textures && textures.size < MAX_TEXTURES) {
                            textures.add(it)
                            textureUpdateNeeded = true
                        }
                    }
                }
            }
        }
        
        // If texture list changed, we eventually need to update all per-frame sets.
        // For the current frame, we check if it matches our last update.
        val currentTexturesHash = textures.fold(0L) { acc, tex -> acc xor tex.view.handle }
        if (lastTextureHandles!![frameIndex] != currentTexturesHash) {
            updateTextureSet(frameIndex)
        }

        // Populate material buffer
        val materialBufferSize = MAX_MATERIALS * 32L
        val matBufferObj = materialBuffers!![frameIndex]
        val matMap = matBufferObj.map()
        val matBuffer = MemoryUtil.memByteBuffer(matMap, materialBufferSize.toInt())
        var matOffset = 0
        
        // We need a stable mapping of material to index for this frame
        val materialToIndex = mutableMapOf<github.businessdirt.axite.vanadium.assets.types.Material, Int>()
        var nextMatIdx = 0

        scene.forEachModel { _: TransformComponent, modelComp: ModelComponent ->
            modelComp.model?.materials?.forEach { mat ->
                if (!materialToIndex.containsKey(mat) && nextMatIdx < MAX_MATERIALS) {
                    materialToIndex[mat] = nextMatIdx
                    
                    if (matOffset + 32 <= materialBufferSize) {
                        matBuffer.putFloat(mat.baseColor.x).putFloat(mat.baseColor.y).putFloat(mat.baseColor.z).putFloat(mat.baseColor.w)
                        val texIdx = textures.indexOf(mat.albedoTexture).coerceAtLeast(0)
                        matBuffer.putInt(if (mat.albedoTexture != null) 1 else 0)
                        matBuffer.putInt(texIdx)
                        matBuffer.putInt(0).putInt(0)
                        matOffset += 32
                        nextMatIdx++
                    }
                }
            }
        }
        matBufferObj.unmap()

        pipeline.bind(commandBuffer)

        val sets = longArrayOf(
            projSets!![frameIndex].handle,
            viewSets!![frameIndex].handle,
            materialSets!![frameIndex].handle,
            textureSets!![frameIndex].handle
        )
        commandBuffer.bindDescriptorSets(pipeline.layout.handle, sets)

        scene.forEachCamera { _: TransformComponent, cameraComp: CameraComponent ->
            val projBuf = projBuffers!![frameIndex]
            val projMap = projBuf.map()
            cameraComp.projectionMatrix.get(0, MemoryUtil.memByteBuffer(projMap, 64))
            projBuf.unmap()

            val viewBuf = viewBuffers!![frameIndex]
            val viewMap = viewBuf.map()
            cameraComp.viewMatrix.get(0, MemoryUtil.memByteBuffer(viewMap, 64))
            viewBuf.unmap()
        }

        fun renderMesh(transform: TransformComponent, mesh: Mesh, modelMaterials: List<github.businessdirt.axite.vanadium.assets.types.Material>) {
            val material = modelMaterials.getOrNull(mesh.materialIndex) ?: return
            val globalMatIdx = materialToIndex[material] ?: 0

            memoryStack { stack ->
                val pcBuffer = stack.malloc(64 + 4)
                transform.globalMatrix.get(0, pcBuffer)
                pcBuffer.putInt(64, globalMatIdx)

                commandBuffer.pushConstants(pipeline.layout.handle, VK_SHADER_STAGE_VERTEX_BIT or VK_SHADER_STAGE_FRAGMENT_BIT, pcBuffer)
                commandBuffer.bindVertexBuffer(mesh.vertexBuffer.handle)
                commandBuffer.bindIndexBuffer(mesh.indexBuffer.handle)
                commandBuffer.drawIndexed(mesh.indexCount)
            }
        }

        // Pass 1: Opaque
        scene.forEachModel { transform: TransformComponent, modelComp: ModelComponent ->
            modelComp.model?.let { model ->
                model.meshes.forEach { mesh ->
                    val material = model.materials.getOrNull(mesh.materialIndex)
                    if (material == null || !material.isTransparent) {
                        renderMesh(transform, mesh, model.materials)
                    }
                }
            }
        }

        // Pass 2: Transparent
        scene.forEachModel { transform: TransformComponent, modelComp: ModelComponent ->
            modelComp.model?.let { model ->
                model.meshes.forEach { mesh ->
                    val material = model.materials.getOrNull(mesh.materialIndex)
                    if (material?.isTransparent == true) {
                        renderMesh(transform, mesh, model.materials)
                    }
                }
            }
        }
    }

    fun shutdown() {
        projSets?.forEach { it.close() }
        viewSets?.forEach { it.close() }
        materialSets?.forEach { it.close() }
        textureSets?.forEach { it.close() }
        descriptorPool?.close()

        projBuffers?.forEach { it.close() }
        viewBuffers?.forEach { it.close() }
        materialBuffers?.forEach { it.close() }

        graphicsPipeline?.close()

        Vanadium.assets.unload(VERTEX_SHADER_PATH)
        Vanadium.assets.unload(FRAGMENT_SHADER_PATH)
        Vanadium.assets.unload(WHITE_TEXTURE_PATH)
    }
}
