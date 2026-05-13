package github.businessdirt.axite.vanadium

import github.businessdirt.axite.vanadium.assets.types.Model
import github.businessdirt.axite.vanadium.assets.types.Shader
import github.businessdirt.axite.vanadium.core.events.Event
import github.businessdirt.axite.vanadium.renderer.SceneRenderer
import github.businessdirt.axite.vanadium.renderer.graph.ClearColorValue
import github.businessdirt.axite.vanadium.renderer.graph.RenderGraph
import github.businessdirt.axite.vanadium.renderer.graph.RenderResourceNames
import github.businessdirt.axite.vanadium.vulkan.commands.*
import github.businessdirt.axite.vanadium.vulkan.pipeline.GraphicsPipeline
import kotlinx.coroutines.CoroutineScope

class VanadiumSandbox : VanadiumAdapter {

    companion object {
        const val FRAGMENT_SHADER_FILE_GLSL: String = "src/sandbox/resources/shaders/scene.frag.glsl"
        const val VERTEX_SHADER_FILE_GLSL: String = "src/sandbox/resources/shaders/scene.vert.glsl"
        const val MODEL_FILE: String = "src/sandbox/resources/models/triangle.obj"
    }

    private var graphicsPipeline: GraphicsPipeline? = null
    private var model: Model? = null

    override suspend fun initialize(scope: CoroutineScope) {
        // Assets are reference counted and pooled. Loading them here increments their ref count.
        val vertexShader = Vanadium.assets.load<Shader>(VERTEX_SHADER_FILE_GLSL)
        val fragmentShader = Vanadium.assets.load<Shader>(FRAGMENT_SHADER_FILE_GLSL)
        model = Vanadium.assets.load<Model>(MODEL_FILE)

        // Create the pipeline using the loaded shaders.
        // The pipeline uses the shader metadata to configure vertex inputs and layout.
        graphicsPipeline = GraphicsPipeline(Vanadium.context.device.handle, vertexShader, fragmentShader)

        vertexShader.release()
        fragmentShader.release()
    }

    override fun shutdown() {
        graphicsPipeline?.close()
        graphicsPipeline = null

        // Unloading assets decrements their ref count and disposes them if it reaches zero.
        Vanadium.assets.unload(MODEL_FILE)
        model = null
    }

    override fun update(frameInfo: FrameInfo) { }

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

                    // Render the model's meshes
                    model?.meshes?.forEach { mesh ->
                        commandBuffer.bindVertexBuffer(mesh.vertexBuffer.handle)
                        commandBuffer.bindIndexBuffer(mesh.indexBuffer.handle)
                        commandBuffer.drawIndexed(mesh.indexCount)
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
                    
                    model?.meshes?.forEach { mesh ->
                        commandBuffer.bindVertexBuffer(mesh.vertexBuffer.handle)
                        commandBuffer.bindIndexBuffer(mesh.indexBuffer.handle)
                        commandBuffer.drawIndexed(mesh.indexCount)
                    }
                }
            }
        }
    }

    override fun onEvent(event: Event) { }
}
