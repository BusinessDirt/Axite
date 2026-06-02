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
import github.businessdirt.axite.vanadium.scene.Scene
import github.businessdirt.axite.vanadium.scene.components.CameraComponent
import github.businessdirt.axite.vanadium.scene.components.CameraControllerComponent
import github.businessdirt.axite.vanadium.scene.components.ControllerSettings
import github.businessdirt.axite.vanadium.scene.components.ModelComponent
import github.businessdirt.axite.vanadium.scene.components.TransformComponent
import github.businessdirt.axite.vanadium.vulkan.commands.*
import github.businessdirt.axite.vanadium.vulkan.descriptors.DescriptorPool
import github.businessdirt.axite.vanadium.vulkan.descriptors.DescriptorSet
import github.businessdirt.axite.vanadium.vulkan.pipeline.*
import kotlinx.coroutines.CoroutineScope
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.vulkan.VK13.*

class VanadiumSandbox : VanadiumAdapter {

    companion object {
        const val FRAGMENT_SHADER_FILE_GLSL: String = "src/sandbox/resources/shaders/scene.frag.glsl"
        const val VERTEX_SHADER_FILE_GLSL: String = "src/sandbox/resources/shaders/scene.vert.glsl"
        const val MODEL_FILE: String = "src/sandbox/resources/models/sponza/Sponza.gltf"
        const val TEXTURE_FILE: String = "src/sandbox/resources/models/sponza/white.png"
    }

    private var graphicsPipeline: GraphicsPipeline? = null
    private val scene = Scene()
    private var sponza: Entity? = null

    private var descriptorPool: DescriptorPool? = null
    private var descriptorSet: DescriptorSet? = null
    private var texture: Texture? = null

    override suspend fun initialize(scope: CoroutineScope) {
        val vertexShader = Vanadium.assets.load<Shader>(VERTEX_SHADER_FILE_GLSL)
        val fragmentShader = Vanadium.assets.load<Shader>(FRAGMENT_SHADER_FILE_GLSL)
        val model = Vanadium.assets.load<Model>(MODEL_FILE)
        texture = Vanadium.assets.load<Texture>(TEXTURE_FILE)

        graphicsPipeline = GraphicsPipeline(Vanadium.context.device.handle) {
            vertexShader(vertexShader)
            fragmentShader(fragmentShader)

            enableBlend = true
        }

        // Initialize Descriptor Set for the texture
        descriptorPool = DescriptorPool(
            Vanadium.context.device.handle, 1, listOf(
                DescriptorPool.PoolSize(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 1)
            )
        )
        
        graphicsPipeline?.layout?.descriptorSetLayouts?.firstOrNull()?.let { layout ->
            descriptorSet = DescriptorSet(Vanadium.context.device.handle, descriptorPool!!, layout)
            texture?.let { tex ->
                descriptorSet?.updateImage(0, tex.view.handle, tex.sampler.handle)
            }
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
        descriptorSet?.close()
        descriptorPool?.close()
        graphicsPipeline?.close()
        graphicsPipeline = null
        scene.close()
        sponza = null

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

                    // Bind Descriptor Set
                    descriptorSet?.let { set ->
                        commandBuffer.bindDescriptorSets(pipeline.layout.handle, longArrayOf(set.handle))
                    }

                    // Find the camera's combined matrix
                    val cameraMatrix = Matrix4f()
                    scene.forEachCamera { _, cameraComp ->
                        cameraMatrix.set(cameraComp.combinedMatrix)
                    }

                    // Render all entities with a ModelComponent using the SceneGraph
                    scene.forEachModel { transform, modelComp ->
                        modelComp.model?.meshes?.forEach { mesh ->
                            memoryStack { stack ->
                                val matrixBuffer = stack.malloc(128)
                                transform.globalMatrix.get(0, matrixBuffer)
                                cameraMatrix.get(64, matrixBuffer)

                                commandBuffer.pushConstants(pipeline.layout.handle, VK_SHADER_STAGE_VERTEX_BIT, matrixBuffer)
                                commandBuffer.bindVertexBuffer(mesh.vertexBuffer.handle)
                                commandBuffer.bindIndexBuffer(mesh.indexBuffer.handle)
                                commandBuffer.drawIndexed(mesh.indexCount)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onEvent(event: Event) { }
}
