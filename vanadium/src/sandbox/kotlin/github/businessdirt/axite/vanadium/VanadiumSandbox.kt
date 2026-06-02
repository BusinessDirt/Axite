package github.businessdirt.axite.vanadium

import github.businessdirt.axite.vanadium.assets.types.Model
import github.businessdirt.axite.vanadium.assets.types.Shader
import github.businessdirt.axite.vanadium.assets.types.Texture
import github.businessdirt.axite.vanadium.core.events.Event
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.renderer.SceneRenderer
import github.businessdirt.axite.vanadium.renderer.graph.ClearColorValue
import github.businessdirt.axite.vanadium.renderer.graph.RenderGraph
import github.businessdirt.axite.vanadium.renderer.graph.RenderResourceNames
import github.businessdirt.axite.vanadium.scene.Entity
import github.businessdirt.axite.vanadium.scene.Mesh
import github.businessdirt.axite.vanadium.scene.Scene
import github.businessdirt.axite.vanadium.scene.components.*
import github.businessdirt.axite.vanadium.vulkan.commands.*
import github.businessdirt.axite.vanadium.vulkan.descriptors.DescriptorPool
import github.businessdirt.axite.vanadium.vulkan.descriptors.DescriptorSet
import github.businessdirt.axite.vanadium.vulkan.pipeline.GraphicsPipeline
import github.businessdirt.axite.vanadium.vulkan.resources.Buffer
import kotlinx.coroutines.CoroutineScope
import org.joml.Vector3f
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK13.*

class VanadiumSandbox : VanadiumAdapter {

    companion object {
        const val FRAGMENT_SHADER_FILE_GLSL: String = "src/sandbox/resources/shaders/scene.frag.glsl"
        const val VERTEX_SHADER_FILE_GLSL: String = "src/sandbox/resources/shaders/scene.vert.glsl"
        const val MODEL_FILE: String = "src/sandbox/resources/models/sponza/Sponza.gltf"
        const val TEXTURE_FILE: String = "src/sandbox/resources/models/sponza/white.png"
        const val MAX_TEXTURES = 16
    }

    private var graphicsPipeline: GraphicsPipeline? = null
    private val scene = Scene()
    private var sponza: Entity? = null

    private var descriptorPool: DescriptorPool? = null
    private var projSet: DescriptorSet? = null
    private var viewSet: DescriptorSet? = null
    private var materialSet: DescriptorSet? = null
    private var textureSet: DescriptorSet? = null

    private var projBuffer: Buffer? = null
    private var viewBuffer: Buffer? = null
    private var materialBuffer: Buffer? = null

    private val textures = mutableListOf<Texture>()

    override suspend fun initialize(scope: CoroutineScope) {
        val vertexShader = Vanadium.assets.load<Shader>(VERTEX_SHADER_FILE_GLSL)
        val fragmentShader = Vanadium.assets.load<Shader>(FRAGMENT_SHADER_FILE_GLSL)
        val model = Vanadium.assets.load<Model>(MODEL_FILE)
        val whiteTexture = Vanadium.assets.load<Texture>(TEXTURE_FILE)

        graphicsPipeline = GraphicsPipeline(Vanadium.context.device.handle) {
            vertexShader(vertexShader)
            fragmentShader(fragmentShader)

            enableBlend = true
        }

        // Collect all textures from the model
        textures.clear()
        textures.add(whiteTexture)
        model.materials.forEach { mat ->
            mat.albedoTexture?.let { if (it !in textures) textures.add(it) }
        }

        // Initialize Descriptor Pool
        descriptorPool = DescriptorPool(
            Vanadium.context.device.handle, 4, listOf(
                DescriptorPool.PoolSize(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 2),
                DescriptorPool.PoolSize(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, 1),
                DescriptorPool.PoolSize(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, MAX_TEXTURES)
            )
        )

        // Create Buffers
        projBuffer = Buffer(Vanadium.context.device.handle, Vanadium.context.physicalDevice, 64, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)
        viewBuffer = Buffer(Vanadium.context.device.handle, Vanadium.context.physicalDevice, 64, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)
        
        val materialBufferSize = model.materials.size.coerceAtLeast(1) * 32L
        materialBuffer = Buffer(Vanadium.context.device.handle, Vanadium.context.physicalDevice, materialBufferSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)

        // Fill Material Buffer
        val matMap = materialBuffer!!.map()
        val matBuffer = MemoryUtil.memByteBuffer(matMap, materialBufferSize.toInt())
        model.materials.forEach { mat ->
            // diffuseColor (vec4)
            matBuffer.putFloat(mat.baseColor.x).putFloat(mat.baseColor.y).putFloat(mat.baseColor.z).putFloat(mat.baseColor.w)
            
            val texIdx = textures.indexOf(mat.albedoTexture).coerceAtLeast(0)
            matBuffer.putInt(if (mat.albedoTexture != null) 1 else 0) // hasTexture
            matBuffer.putInt(texIdx) // textureIdx
            matBuffer.putInt(0).putInt(0) // padding
        }
        materialBuffer!!.unmap()

        // Allocate and Update Descriptor Sets
        graphicsPipeline?.layout?.descriptorSetLayouts?.let { layouts ->
            projSet = DescriptorSet(Vanadium.context.device.handle, descriptorPool!!, layouts[0])
            projSet?.updateBuffer(0, projBuffer!!.handle, 64)

            viewSet = DescriptorSet(Vanadium.context.device.handle, descriptorPool!!, layouts[1])
            viewSet?.updateBuffer(0, viewBuffer!!.handle, 64)

            materialSet = DescriptorSet(Vanadium.context.device.handle, descriptorPool!!, layouts[2])
            materialSet?.updateBuffer(0, materialBuffer!!.handle, materialBufferSize, type = VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)

            textureSet = DescriptorSet(Vanadium.context.device.handle, descriptorPool!!, layouts[3])
            val views = LongArray(MAX_TEXTURES) { whiteTexture.view.handle }
            val samplers = LongArray(MAX_TEXTURES) { whiteTexture.sampler.handle }
            textures.take(MAX_TEXTURES).forEachIndexed { i, tex ->
                views[i] = tex.view.handle
                samplers[i] = tex.sampler.handle
            }
            textureSet?.updateImages(0, views, samplers)
        }

        // Create a sponza entity
        sponza = scene.createEntity("Sponza").apply {
            configure {
                it += ModelComponent(model)
                it += TransformComponent(
                    position = Vector3f(0f, 0f, 0f),
                    scale = Vector3f(1f, 1f, 1f)
                )
            }
        }

        // Create a camera entity
        scene.createEntity("Camera").apply {
            configure {
                it += CameraComponent()
                it += CameraControllerComponent(
                    settings = ControllerSettings.FirstPerson(speed = 10f)
                )
                it += TransformComponent(
                    position = Vector3f(0f, 2f, 0f)
                )
            }
        }
    }

    override fun shutdown() {
        projSet?.close()
        viewSet?.close()
        materialSet?.close()
        textureSet?.close()
        descriptorPool?.close()

        projBuffer?.close()
        viewBuffer?.close()
        materialBuffer?.close()

        graphicsPipeline?.close()
        graphicsPipeline = null
        scene.close()
        sponza = null
        textures.clear()

        Vanadium.assets.unload(VERTEX_SHADER_FILE_GLSL)
        Vanadium.assets.unload(FRAGMENT_SHADER_FILE_GLSL)
        Vanadium.assets.unload(MODEL_FILE)
        Vanadium.assets.unload(TEXTURE_FILE)
    }

    override fun update(frameInfo: FrameInfo) {
        scene.update(frameInfo.deltaTime.toFloat())
    }

    override fun onRecord(graph: RenderGraph, sceneRenderer: SceneRenderer, commandBuffer: CommandBuffer, interpolation: Double) = graph.build {
        pass("MainScenePass") {
            writes(RenderResourceNames.BACK_BUFFER, RenderResourceNames.DEPTH_BUFFER)

            clearColor = ClearColorValue(0.4f, 0.6f, 0.9f, 1.0f)
            clearDepth = 1.0f

            pipeline { commandBuffer ->
                graphicsPipeline?.let { pipeline ->
                    pipeline.bind(commandBuffer)

                    // Bind all Descriptor Sets
                    val sets = longArrayOf(
                        projSet?.handle ?: 0L,
                        viewSet?.handle ?: 0L,
                        materialSet?.handle ?: 0L,
                        textureSet?.handle ?: 0L
                    )
                    commandBuffer.bindDescriptorSets(pipeline.layout.handle, sets)

                    // Update Proj and View buffers
                    scene.forEachCamera { _, cameraComp ->
                        val projMap = projBuffer!!.map()
                        cameraComp.projectionMatrix.get(0, MemoryUtil.memByteBuffer(projMap, 64))
                        projBuffer!!.unmap()

                        val viewMap = viewBuffer!!.map()
                        cameraComp.viewMatrix.get(0, MemoryUtil.memByteBuffer(viewMap, 64))
                        viewBuffer!!.unmap()
                    }

                    // Render all entities with a ModelComponent using the SceneGraph
                    // Helper to render a mesh
                    fun renderMesh(transform: TransformComponent, mesh: Mesh) {
                        memoryStack { stack ->
                            // Push Constants: Model Matrix (0-63), materialIdx (64-67)
                            val pcBuffer = stack.malloc(64 + 4)
                            transform.globalMatrix.get(0, pcBuffer)
                            pcBuffer.putInt(64, mesh.materialIndex)

                            commandBuffer.pushConstants(pipeline.layout.handle, VK_SHADER_STAGE_VERTEX_BIT or VK_SHADER_STAGE_FRAGMENT_BIT, pcBuffer)
                            commandBuffer.bindVertexBuffer(mesh.vertexBuffer.handle)
                            commandBuffer.bindIndexBuffer(mesh.indexBuffer.handle)
                            commandBuffer.drawIndexed(mesh.indexCount)
                        }
                    }

                    // Pass 1: Opaque
                    scene.forEachModel { transform, modelComp ->
                        modelComp.model?.let { model ->
                            model.meshes.forEach { mesh ->
                                val material = model.materials.getOrNull(mesh.materialIndex)
                                if (material == null || !material.isTransparent) {
                                    renderMesh(transform, mesh)
                                }
                            }
                        }
                    }

                    // Pass 2: Transparent
                    scene.forEachModel { transform, modelComp ->
                        modelComp.model?.let { model ->
                            model.meshes.forEach { mesh ->
                                val material = model.materials.getOrNull(mesh.materialIndex)
                                if (material?.isTransparent == true) {
                                    renderMesh(transform, mesh)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onEvent(event: Event) { }
}
