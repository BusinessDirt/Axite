package github.businessdirt.axite.vanadium

import github.businessdirt.axite.vanadium.assets.types.Model
import github.businessdirt.axite.vanadium.assets.types.Shader
import github.businessdirt.axite.vanadium.core.events.Event
import github.businessdirt.axite.vanadium.renderer.SceneRenderer
import github.businessdirt.axite.vanadium.renderer.graph.ClearColorValue
import github.businessdirt.axite.vanadium.renderer.graph.RenderGraph
import github.businessdirt.axite.vanadium.renderer.graph.RenderResourceNames
import github.businessdirt.axite.vanadium.scene.ModelComponent
import github.businessdirt.axite.vanadium.scene.SceneGraph
import github.businessdirt.axite.vanadium.scene.TransformComponent
import github.businessdirt.axite.vanadium.vulkan.commands.*
import github.businessdirt.axite.vanadium.vulkan.pipeline.GraphicsPipeline
import kotlinx.coroutines.CoroutineScope
import org.joml.Vector3f

class VanadiumSandbox : VanadiumAdapter {

    companion object {
        const val FRAGMENT_SHADER_FILE_GLSL: String = "src/sandbox/resources/shaders/scene.frag.glsl"
        const val VERTEX_SHADER_FILE_GLSL: String = "src/sandbox/resources/shaders/scene.vert.glsl"
        const val MODEL_FILE: String = "src/sandbox/resources/models/triangle.obj"
    }

    private var graphicsPipeline: GraphicsPipeline? = null
    private val sceneGraph = SceneGraph()

    override suspend fun initialize(scope: CoroutineScope) {
        val vertexShader = Vanadium.assets.load<Shader>(VERTEX_SHADER_FILE_GLSL)
        val fragmentShader = Vanadium.assets.load<Shader>(FRAGMENT_SHADER_FILE_GLSL)
        val model = Vanadium.assets.load<Model>(MODEL_FILE)

        graphicsPipeline = GraphicsPipeline(Vanadium.context.device.handle, vertexShader, fragmentShader)

        // Create an entity in the scene graph
        sceneGraph.createEntity("Triangle").apply {
            configure {
                it += ModelComponent(model)
                it += TransformComponent(
                    position = Vector3f(0f, 0f, 0f),
                    scale = Vector3f(1.0f, 1.0f, 1.0f)
                )
            }
        }
    }

    override fun shutdown() {
        graphicsPipeline?.close()
        graphicsPipeline = null
        sceneGraph.close()

        Vanadium.assets.unload(VERTEX_SHADER_FILE_GLSL)
        Vanadium.assets.unload(FRAGMENT_SHADER_FILE_GLSL)
        Vanadium.assets.unload(MODEL_FILE)
    }

    override fun update(frameInfo: FrameInfo) {
        sceneGraph.update(frameInfo.deltaTime.toFloat())
    }

    override fun onRecord(graph: RenderGraph, sceneRenderer: SceneRenderer, commandBuffer: CommandBuffer, interpolation: Double) = graph.build {
        val fbWidth = Vanadium.context.swapchain.extent.width()
        val fbHeight = Vanadium.context.swapchain.extent.height()

        pass("MainScenePass") {
            writes(RenderResourceNames.BACK_BUFFER, RenderResourceNames.DEPTH_BUFFER)
            clearColor = ClearColorValue(0.4f, 0.6f, 0.9f, 1.0f)
            clearDepth = 1.0f

            pipeline { commandBuffer ->
                graphicsPipeline?.let { pipeline ->
                    pipeline.bind(commandBuffer)

                    // Render all entities with a ModelComponent using the SceneGraph
                    sceneGraph.forEachModel { transform, modelComp ->
                        modelComp.model?.meshes?.forEach { mesh ->
                            // In a real engine, we'd pass the transform.globalMatrix to a push constant or UBO
                            commandBuffer.bindVertexBuffer(mesh.vertexBuffer.handle)
                            commandBuffer.bindIndexBuffer(mesh.indexBuffer.handle)
                            commandBuffer.drawIndexed(mesh.indexCount)
                        }
                    }
                }
            }
        }

        pass("DebugScenePass") {
            writes(RenderResourceNames.BACK_BUFFER, RenderResourceNames.DEPTH_BUFFER)
            pipeline { commandBuffer ->
                commandBuffer.setScissor(fbWidth / 2, fbHeight / 2, fbWidth / 2, fbHeight / 2)
                commandBuffer.setViewport((fbWidth / 2).toFloat(), (fbHeight / 2).toFloat(), (fbWidth / 2).toFloat(), (fbHeight / 2).toFloat())

                graphicsPipeline?.let { pipeline ->
                    pipeline.bind(commandBuffer)
                    
                    sceneGraph.forEachModel { _, modelComp ->
                        modelComp.model?.meshes?.forEach { mesh ->
                            commandBuffer.bindVertexBuffer(mesh.vertexBuffer.handle)
                            commandBuffer.bindIndexBuffer(mesh.indexBuffer.handle)
                            commandBuffer.drawIndexed(mesh.indexCount)
                        }
                    }
                }
            }
        }
    }

    override fun onEvent(event: Event) { }
}
