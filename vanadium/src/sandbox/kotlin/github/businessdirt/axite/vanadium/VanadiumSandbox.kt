package github.businessdirt.axite.vanadium

import github.businessdirt.axite.vanadium.assets.types.Model
import github.businessdirt.axite.vanadium.core.events.Event
import github.businessdirt.axite.vanadium.core.imgui.ImGuiUtils.dockspace
import github.businessdirt.axite.vanadium.core.imgui.panels.RenderPassesPanel
import github.businessdirt.axite.vanadium.renderer.graph.RenderGraph
import github.businessdirt.axite.vanadium.renderer.passes.GeometryPass
import github.businessdirt.axite.vanadium.renderer.passes.ImGuiPass
import github.businessdirt.axite.vanadium.renderer.passes.PostProcessPass
import github.businessdirt.axite.vanadium.scene.Scene
import github.businessdirt.axite.vanadium.scene.components.*
import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer
import kotlinx.coroutines.CoroutineScope
import org.joml.Vector3f

class VanadiumSandbox : VanadiumAdapter {

    companion object {
        const val MODEL_FILE: String = "src/sandbox/resources/models/sponza/Sponza.gltf"
    }

    private val scene = Scene()

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

        GeometryPass.initialize()
        ImGuiPass.initialize()
        PostProcessPass.apply {
            initialize()
            useFXAA = true
        }
    }

    override fun shutdown() {
        scene.close()
        Vanadium.assets.unload(MODEL_FILE)
    }

    override fun update(frameInfo: FrameInfo) {
        scene.update(frameInfo.deltaTime.toFloat())
    }

    override fun onRecord(graph: RenderGraph, commandBuffer: CommandBuffer, interpolation: Double) = graph.build {
        val sceneColor = GeometryPass.record(this, scene, "scene_color")
        val postColor = PostProcessPass.record(this, sceneColor)

        ImGuiPass.record(this, postColor) {
            dockspace()
            RenderPassesPanel.draw()
        }
    }

    override fun onEvent(event: Event) { }
}
