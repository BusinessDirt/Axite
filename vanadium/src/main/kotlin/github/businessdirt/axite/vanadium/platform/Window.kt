package github.businessdirt.axite.vanadium.platform

import github.businessdirt.axite.vanadium.VanadiumConfig
import github.businessdirt.axite.vanadium.events.glfw.*
import github.businessdirt.axite.vanadium.math.Resolution
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported
import org.lwjgl.system.MemoryUtil.NULL

class Window(config: VanadiumConfig) : AutoCloseable {

    val handle: Long

    var width: Int
        private set
    var height: Int
        private set

    var shouldClose: Boolean
        get() = glfwWindowShouldClose(handle)
        set(value) = glfwSetWindowShouldClose(handle, value)

    init {
        check(glfwInit()) { "Unable to initialize GLFW" }
        check(glfwVulkanSupported()) { "Cannot find a compatible Vulkan installable client driver (ICD)" }

        if (config.resolution.isInvalid) {
            val vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor())
                ?: error("Error getting primary monitor")
            config.resolution = Resolution(vidMode.width(), vidMode.height())
        }

        width = config.resolution.width
        height = config.resolution.height

        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_FALSE)

        // Create the window
        handle = glfwCreateWindow(width, height, config.applicationName, NULL, NULL)
        check(handle != NULL) { "Failed to create the GLFW window" }

        // Window Resize
        glfwSetFramebufferSizeCallback(handle) { _, w, h ->
            width = w
            height = h
            WindowResizedEvent(w, h).post()
        }

        // Keyboard Input
        glfwSetKeyCallback(handle) { _, key, _, action, mods ->
            // Using Kotlin's 'when' expression for clean control flow
            when (action) {
                GLFW_PRESS, GLFW_REPEAT -> KeyPressedEvent(key, mods).post()
                GLFW_RELEASE -> KeyReleasedEvent(key, mods).post()
            }
        }

        // Mouse Position
        glfwSetCursorPosCallback(handle) { _, x, y ->
            MouseMovedEvent(x, y).post()
        }

        // Mouse Buttons
        glfwSetMouseButtonCallback(handle) { _, button, action, mods ->
            when (action) {
                GLFW_PRESS -> MousePressedEvent(button, mods).post()
                GLFW_RELEASE -> MouseReleasedEvent(button, mods).post()
            }
        }
    }

    fun pollEvents() = glfwPollEvents()

    override fun close() {
        glfwFreeCallbacks(handle)
        glfwDestroyWindow(handle)
        glfwTerminate()
    }
}