package github.businessdirt.axite.vanadium

import github.businessdirt.axite.vanadium.assets.types.Model
import github.businessdirt.axite.vanadium.core.events.Event
import github.businessdirt.axite.vanadium.renderer.passes.GeometryPass
import github.businessdirt.axite.vanadium.renderer.passes.PostProcessPass
import github.businessdirt.axite.vanadium.renderer.graph.ClearColorValue
import github.businessdirt.axite.vanadium.renderer.graph.RenderGraph
import github.businessdirt.axite.vanadium.renderer.graph.RenderResourceNames
import github.businessdirt.axite.vanadium.scene.Scene
import github.businessdirt.axite.vanadium.scene.components.*
import github.businessdirt.axite.vanadium.vulkan.commands.*
import kotlinx.coroutines.CoroutineScope
import org.joml.Vector3f
import org.lwjgl.vulkan.VK13.*

class VanadiumSandbox : VanadiumAdapter {

    companion object {
        const val MODEL_FILE: String = "src/sandbox/resources/models/sponza/Sponza.gltf"
        const val SCENE_COLOR: String = "scene_color"
    }

    private val scene = Scene()
    private lateinit var postPass: PostProcessPass

    override suspend fun initialize(scope: CoroutineScope) {
        val model = Vanadium.assets.load<Model>(MODEL_FILE)

        // Create a sponza entity
        scene.createEntity("Sponza").apply {
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

        postPass = PostProcessPass(Vanadium.context).also {
            it.initialize()
            it.useFXAA = true
        }
    }

    override fun shutdown() {
        postPass.shutdown()
        scene.close()
        Vanadium.assets.unload(MODEL_FILE)
    }

    override fun update(frameInfo: FrameInfo) {
        scene.update(frameInfo.deltaTime.toFloat())
    }

    override fun onRecord(graph: RenderGraph, geometryPass: GeometryPass, commandBuffer: CommandBuffer, interpolation: Double) = graph.build {
        val backBuffer = graph.registry[RenderResourceNames.BACK_BUFFER]

        // Ensure intermediate buffer exists and matches backbuffer size
        try {
            val intermediate = graph.registry[SCENE_COLOR]
            if (intermediate.width != backBuffer.width || intermediate.height != backBuffer.height) {
                throw Exception()
            }
        } catch (e: Exception) {
            graph.registry.createResource(
                SCENE_COLOR,
                backBuffer.width,
                backBuffer.height,
                Vanadium.context.surface.surfaceFormat.imageFormat,
                VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT or VK_IMAGE_USAGE_SAMPLED_BIT
            )
        }

        val sceneColor = geometryPass.record(this, scene, SCENE_COLOR)
        postPass.record(this, sceneColor, RenderResourceNames.BACK_BUFFER)
    }

    override fun onEvent(event: Event) { }
}
