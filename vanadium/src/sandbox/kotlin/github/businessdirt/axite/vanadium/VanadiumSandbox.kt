package github.businessdirt.axite.vanadium

import github.businessdirt.axite.vanadium.assets.types.Model
import github.businessdirt.axite.vanadium.assets.types.Shader
import github.businessdirt.axite.vanadium.core.events.Event
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.renderer.SceneRenderer
import github.businessdirt.axite.vanadium.renderer.graph.ClearColorValue
import github.businessdirt.axite.vanadium.renderer.graph.RenderGraph
import github.businessdirt.axite.vanadium.renderer.graph.RenderResourceNames
import github.businessdirt.axite.vanadium.scene.Scene
import github.businessdirt.axite.vanadium.scene.components.CameraComponent
import github.businessdirt.axite.vanadium.scene.components.ModelComponent
import github.businessdirt.axite.vanadium.scene.components.TransformComponent
import github.businessdirt.axite.vanadium.vulkan.commands.*
import github.businessdirt.axite.vanadium.vulkan.pipeline.GraphicsPipeline
import kotlinx.coroutines.CoroutineScope
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.vulkan.VK13.VK_SHADER_STAGE_VERTEX_BIT

class VanadiumSandbox : VanadiumAdapter {

    companion object {
        const val FRAGMENT_SHADER_FILE_GLSL: String = "src/sandbox/resources/shaders/scene.frag.glsl"
        const val VERTEX_SHADER_FILE_GLSL: String = "src/sandbox/resources/shaders/scene.vert.glsl"
        const val MODEL_FILE: String = "src/sandbox/resources/models/cube.obj"
    }

    private var graphicsPipeline: GraphicsPipeline? = null
    private val scene = Scene()
    private var cube: github.businessdirt.axite.vanadium.scene.Entity? = null

    override suspend fun initialize(scope: CoroutineScope) {
        val vertexShader = Vanadium.assets.load<Shader>(VERTEX_SHADER_FILE_GLSL)
        val fragmentShader = Vanadium.assets.load<Shader>(FRAGMENT_SHADER_FILE_GLSL)
        val model = Vanadium.assets.load<Model>(MODEL_FILE)

        graphicsPipeline = GraphicsPipeline(Vanadium.context.device.handle, vertexShader, fragmentShader)

        // Create a cube entity
        cube = scene.createEntity("Cube").apply {
            configure {
                it += ModelComponent(model)
                it += TransformComponent(
                    position = Vector3f(0f, 0f, -5f),
                    scale = Vector3f(1.0f, 1.0f, 1.0f)
                )
            }
        }

        // Create a camera entity
        scene.createEntity("Camera").apply {
            configure {
                it += CameraComponent()
                it += TransformComponent(
                    position = Vector3f(0f, 0f, 0f)
                )
            }
        }
    }

    override fun shutdown() {
        graphicsPipeline?.close()
        graphicsPipeline = null
        scene.close()
        cube = null

        Vanadium.assets.unload(VERTEX_SHADER_FILE_GLSL)
        Vanadium.assets.unload(FRAGMENT_SHADER_FILE_GLSL)
        Vanadium.assets.unload(MODEL_FILE)
    }

    override fun update(frameInfo: FrameInfo) {
        cube?.let { entity ->
            val transform = entity[TransformComponent]
            val delta = frameInfo.deltaTime.toFloat()
            transform.rotation.rotateXYZ(delta * 1.0f, delta * 0.8f, delta * 0.6f)
        }
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
