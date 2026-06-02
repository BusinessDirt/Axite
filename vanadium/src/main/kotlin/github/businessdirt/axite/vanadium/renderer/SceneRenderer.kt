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
        const val MAX_TEXTURES = 16
        const val MAX_MATERIALS = 1024

        const val VERTEX_SHADER_PATH = "src/main/resources/shaders/scene.vert.glsl"
        const val FRAGMENT_SHADER_PATH = "src/main/resources/shaders/scene.frag.glsl"
        const val WHITE_TEXTURE_PATH = "src/main/resources/textures/white.png"
    }

    private var graphicsPipeline: GraphicsPipeline? = null
    private var descriptorPool: DescriptorPool? = null
    private var projSet: DescriptorSet? = null
    private var viewSet: DescriptorSet? = null
    private var materialSet: DescriptorSet? = null
    private var textureSet: DescriptorSet? = null

    private var projBuffer: Buffer? = null
    private var viewBuffer: Buffer? = null
    private var materialBuffer: Buffer? = null

    private val textures = mutableListOf<Texture>()
    private lateinit var whiteTexture: Texture

    suspend fun initialize() = Profiler.profile("SceneRenderer Initialization") {
        val vertexShader = Vanadium.assets.load<Shader>(VERTEX_SHADER_PATH)
        val fragmentShader = Vanadium.assets.load<Shader>(FRAGMENT_SHADER_PATH)
        whiteTexture = Vanadium.assets.load<Texture>(WHITE_TEXTURE_PATH)

        graphicsPipeline = GraphicsPipeline(context.device.handle) {
            vertexShader(vertexShader)
            fragmentShader(fragmentShader)
            enableBlend = true
        }

        descriptorPool = DescriptorPool(
            context.device.handle, 4, listOf(
                DescriptorPool.PoolSize(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 2),
                DescriptorPool.PoolSize(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, 1),
                DescriptorPool.PoolSize(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, MAX_TEXTURES)
            )
        )

        projBuffer = Buffer(context.device.handle, context.physicalDevice, 64, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)
        viewBuffer = Buffer(context.device.handle, context.physicalDevice, 64, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)

        val materialBufferSize = MAX_MATERIALS * 32L
        materialBuffer = Buffer(context.device.handle, context.physicalDevice, materialBufferSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)

        graphicsPipeline?.layout?.descriptorSetLayouts?.let { layouts ->
            projSet = DescriptorSet(context.device.handle, descriptorPool!!, layouts[0])
            projSet?.updateBuffer(0, projBuffer!!.handle, 64)

            viewSet = DescriptorSet(context.device.handle, descriptorPool!!, layouts[1])
            viewSet?.updateBuffer(0, viewBuffer!!.handle, 64)

            materialSet = DescriptorSet(context.device.handle, descriptorPool!!, layouts[2])
            materialSet?.updateBuffer(0, materialBuffer!!.handle, materialBufferSize, type = VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)

            textureSet = DescriptorSet(context.device.handle, descriptorPool!!, layouts[3])
            updateTextureSet()
        }
    }

    private fun updateTextureSet() {
        val views = LongArray(MAX_TEXTURES) { whiteTexture.view.handle }
        val samplers = LongArray(MAX_TEXTURES) { whiteTexture.sampler.handle }
        textures.take(MAX_TEXTURES).forEachIndexed { i, tex ->
            views[i] = tex.view.handle
            samplers[i] = tex.sampler.handle
        }
        textureSet?.updateImages(0, views, samplers)
    }

    fun render(commandBuffer: CommandBuffer, scene: Scene) {
        val pipeline = graphicsPipeline ?: return

        // Update texture list and material buffer from scene
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
        if (textureUpdateNeeded) updateTextureSet()

        val materialBufferSize = MAX_MATERIALS * 32L
        val matMap = materialBuffer!!.map()
        val matBuffer = MemoryUtil.memByteBuffer(matMap, materialBufferSize.toInt())
        var matOffset = 0
        scene.forEachModel { _: TransformComponent, modelComp: ModelComponent ->
            modelComp.model?.materials?.forEach { mat ->
                if (matOffset + 32 <= materialBufferSize) {
                    matBuffer.putFloat(mat.baseColor.x).putFloat(mat.baseColor.y).putFloat(mat.baseColor.z).putFloat(mat.baseColor.w)
                    val texIdx = textures.indexOf(mat.albedoTexture).coerceAtLeast(0)
                    matBuffer.putInt(if (mat.albedoTexture != null) 1 else 0)
                    matBuffer.putInt(texIdx)
                    matBuffer.putInt(0).putInt(0)
                    matOffset += 32
                }
            }
        }
        materialBuffer!!.unmap()

        pipeline.bind(commandBuffer)

        val sets = longArrayOf(
            projSet?.handle ?: 0L,
            viewSet?.handle ?: 0L,
            materialSet?.handle ?: 0L,
            textureSet?.handle ?: 0L
        )
        commandBuffer.bindDescriptorSets(pipeline.layout.handle, sets)

        scene.forEachCamera { _: TransformComponent, cameraComp: CameraComponent ->
            val projMap = projBuffer!!.map()
            cameraComp.projectionMatrix.get(0, MemoryUtil.memByteBuffer(projMap, 64))
            projBuffer!!.unmap()

            val viewMap = viewBuffer!!.map()
            cameraComp.viewMatrix.get(0, MemoryUtil.memByteBuffer(viewMap, 64))
            viewBuffer!!.unmap()
        }

        fun renderMesh(transform: TransformComponent, mesh: Mesh, matBaseIdx: Int) {
            memoryStack { stack ->
                val pcBuffer = stack.malloc(64 + 4)
                transform.globalMatrix.get(0, pcBuffer)
                pcBuffer.putInt(64, matBaseIdx + mesh.materialIndex)

                commandBuffer.pushConstants(pipeline.layout.handle, VK_SHADER_STAGE_VERTEX_BIT or VK_SHADER_STAGE_FRAGMENT_BIT, pcBuffer)
                commandBuffer.bindVertexBuffer(mesh.vertexBuffer.handle)
                commandBuffer.bindIndexBuffer(mesh.indexBuffer.handle)
                commandBuffer.drawIndexed(mesh.indexCount)
            }
        }

        var currentMatBaseIdx = 0
        // Pass 1: Opaque
        scene.forEachModel { transform: TransformComponent, modelComp: ModelComponent ->
            modelComp.model?.let { model ->
                model.meshes.forEach { mesh ->
                    val material = model.materials.getOrNull(mesh.materialIndex)
                    if (material == null || !material.isTransparent) {
                        renderMesh(transform, mesh, currentMatBaseIdx)
                    }
                }
                currentMatBaseIdx += model.materials.size
            }
        }

        currentMatBaseIdx = 0
        // Pass 2: Transparent
        scene.forEachModel { transform: TransformComponent, modelComp: ModelComponent ->
            modelComp.model?.let { model ->
                model.meshes.forEach { mesh ->
                    val material = model.materials.getOrNull(mesh.materialIndex)
                    if (material?.isTransparent == true) {
                        renderMesh(transform, mesh, currentMatBaseIdx)
                    }
                }
                currentMatBaseIdx += model.materials.size
            }
        }
    }

    fun shutdown() {
        projSet?.close()
        viewSet?.close()
        materialSet?.close()
        textureSet?.close()
        descriptorPool?.close()

        projBuffer?.close()
        viewBuffer?.close()
        materialBuffer?.close()

        graphicsPipeline?.close()
    }
}
