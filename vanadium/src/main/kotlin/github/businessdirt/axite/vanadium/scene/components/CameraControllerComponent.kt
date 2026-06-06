package github.businessdirt.axite.vanadium.scene.components

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import github.businessdirt.axite.vanadium.platform.KeyboardInput
import github.businessdirt.axite.vanadium.platform.MouseInput
import github.businessdirt.axite.vanadium.scene.Entity
import imgui.ImGui
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*

data class CameraControllerComponent(
    var settings: ControllerSettings = ControllerSettings.FirstPerson()
) : Component<CameraControllerComponent> {
    override fun type() = CameraControllerComponent
    companion object : ComponentType<CameraControllerComponent>()
}

sealed class ControllerSettings {
    abstract fun update(transform: TransformComponent, deltaTime: Float)

    data class FirstPerson(
        var sensitivity: Float = 0.1f,
        var speed: Float = 5f,
        var pitch: Float = 0f,
        var yaw: Float = 0f
    ) : ControllerSettings() {
        override fun update(transform: TransformComponent, deltaTime: Float) {
            // Mouse look
            if (!ImGui.getIO().wantCaptureMouse) {
                yaw += MouseInput.dx.toFloat() * sensitivity
                pitch -= MouseInput.dy.toFloat() * sensitivity
                pitch = pitch.coerceIn(-89f, 89f)
            }

            transform.rotation.identity()
                .rotateY(Math.toRadians(-yaw.toDouble()).toFloat())
                .rotateX(Math.toRadians(pitch.toDouble()).toFloat())

            // Keyboard movement
            if (!ImGui.getIO().wantCaptureKeyboard) {
                val direction = Vector3f(0f, 0f, 0f)
                val forward = Vector3f(0f, 0f, -1f).rotate(transform.rotation)
                val right = Vector3f(1f, 0f, 0f).rotate(transform.rotation)

                if (KeyboardInput.isKeyPressed(GLFW_KEY_W)) direction.add(forward)
                if (KeyboardInput.isKeyPressed(GLFW_KEY_S)) direction.sub(forward)
                if (KeyboardInput.isKeyPressed(GLFW_KEY_A)) direction.sub(right)
                if (KeyboardInput.isKeyPressed(GLFW_KEY_D)) direction.add(right)

                if (direction.lengthSquared() > 0) {
                    direction.normalize().mul(speed * deltaTime)
                    transform.position.add(direction)
                }
            }
        }
    }

    data class ThirdPerson(
        var target: Entity? = null,
        var distance: Float = 5f,
        var orbitSpeed: Float = 0.1f,
        var pitch: Float = 45f,
        var yaw: Float = 0f
    ) : ControllerSettings() {
        override fun update(transform: TransformComponent, deltaTime: Float) {
            // Orbit logic
            if (!ImGui.getIO().wantCaptureMouse) {
                if (MouseInput.isButtonPressed(GLFW_MOUSE_BUTTON_RIGHT)) {
                    yaw += MouseInput.dx.toFloat() * orbitSpeed
                    pitch -= MouseInput.dy.toFloat() * orbitSpeed
                    pitch = pitch.coerceIn(5f, 85f)
                }

                distance -= MouseInput.scrollY.toFloat()
                distance = distance.coerceIn(1f, 100f)
            }

            val targetPos = target?.get(TransformComponent)?.position ?: Vector3f(0f, 0f, 0f)

            val offset = Vector3f(0f, 0f, distance)
            val rotation = Quaternionf()
                .rotateY(Math.toRadians(-yaw.toDouble()).toFloat())
                .rotateX(Math.toRadians(pitch.toDouble()).toFloat())

            offset.rotate(rotation)

            transform.position.set(targetPos).add(offset)
            transform.rotation.set(rotation)
        }
    }

    data class FreeFly(
        var sensitivity: Float = 0.1f,
        var speed: Float = 5f,
        var pitch: Float = 0f,
        var yaw: Float = 0f
    ) : ControllerSettings() {
        override fun update(transform: TransformComponent, deltaTime: Float) {
            // Mouse look
            if (!ImGui.getIO().wantCaptureMouse) {
                yaw += MouseInput.dx.toFloat() * sensitivity
                pitch -= MouseInput.dy.toFloat() * sensitivity
                pitch = pitch.coerceIn(-89f, 89f)
            }

            transform.rotation.identity()
                .rotateY(Math.toRadians(-yaw.toDouble()).toFloat())
                .rotateX(Math.toRadians(pitch.toDouble()).toFloat())

            // Keyboard movement
            if (!ImGui.getIO().wantCaptureKeyboard) {
                val direction = Vector3f(0f, 0f, 0f)
                val forward = Vector3f(0f, 0f, -1f).rotate(transform.rotation)
                val right = Vector3f(1f, 0f, 0f).rotate(transform.rotation)
                val up = Vector3f(0f, 1f, 0f) // Global up for fly

                if (KeyboardInput.isKeyPressed(GLFW_KEY_W)) direction.add(forward)
                if (KeyboardInput.isKeyPressed(GLFW_KEY_S)) direction.sub(forward)
                if (KeyboardInput.isKeyPressed(GLFW_KEY_A)) direction.sub(right)
                if (KeyboardInput.isKeyPressed(GLFW_KEY_D)) direction.add(right)
                if (KeyboardInput.isKeyPressed(GLFW_KEY_SPACE)) direction.add(up)
                if (KeyboardInput.isKeyPressed(GLFW_KEY_LEFT_SHIFT)) direction.sub(up)

                if (direction.lengthSquared() > 0) {
                    direction.normalize().mul(speed * deltaTime)
                    transform.position.add(direction)
                }
            }
        }
    }
}