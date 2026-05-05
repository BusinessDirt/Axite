package github.businessdirt.axite.vanadium.platform

import github.businessdirt.axite.vanadium.VanadiumAdapter
import github.businessdirt.axite.vanadium.VanadiumConfig
import github.businessdirt.axite.vanadium.core.events.*
import github.businessdirt.axite.vanadium.core.profiling.Profiler
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryUtil.NULL
import org.slf4j.LoggerFactory

class Window(private val config: VanadiumConfig) {
    private val logger = LoggerFactory.getLogger(Window::class.java)

    var handle: Long = NULL
        private set

    /**
     * Initializes the window on the current thread.
     * MUST be called from the main thread.
     */
    fun initialize(adapter: VanadiumAdapter) = Profiler.profile("Window Initialization") {
        Profiler.profile("GLFW Window Creation") {
            // Configure Window Hints for Vulkan
            glfwDefaultWindowHints()
            glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
            glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)

            handle = glfwCreateWindow(
                config.resolution.width,
                config.resolution.height,
                config.applicationName,
                NULL,
                NULL
            )

            if (handle == NULL) throw RuntimeException("Failed to create the GLFW window")
        }

        Profiler.profile("Event Callbacks Setup") {
            // --- Window / App Events ---
            glfwSetWindowSizeCallback(handle) { _, w, h ->
                adapter.onEvent(WindowResizedEvent(w, h))
            }

            glfwSetWindowCloseCallback(handle) { _ ->
                val event = WindowClosedEvent()
                adapter.onEvent(event)
                if (event.isCancelled) {
                    glfwSetWindowShouldClose(handle, false)
                }
            }

            glfwSetWindowFocusCallback(handle) { _, focused ->
                adapter.onEvent(WindowFocusEvent(focused))
            }

            glfwSetWindowPosCallback(handle) { _, x, y ->
                adapter.onEvent(WindowMovedEvent(x, y))
            }

            // --- Keyboard Events ---
            glfwSetKeyCallback(handle) { _, key, _, action, _ ->
                when (action) {
                    GLFW_PRESS -> adapter.onEvent(KeyPressedEvent(key, 0))
                    GLFW_REPEAT -> adapter.onEvent(KeyPressedEvent(key, 1))
                    GLFW_RELEASE -> adapter.onEvent(KeyReleasedEvent(key))
                }
            }

            glfwSetCharCallback(handle) { _, codepoint ->
                adapter.onEvent(KeyTypedEvent(codepoint.toChar()))
            }

            // --- Mouse Events ---
            glfwSetCursorPosCallback(handle) { _, x, y ->
                adapter.onEvent(MouseMovedEvent(x, y))
            }

            glfwSetMouseButtonCallback(handle) { _, button, action, _ ->
                when (action) {
                    GLFW_PRESS -> adapter.onEvent(MouseButtonPressedEvent(button))
                    GLFW_RELEASE -> adapter.onEvent(MouseButtonReleasedEvent(button))
                }
            }

            glfwSetScrollCallback(handle) { _, xOffset, yOffset ->
                adapter.onEvent(MouseScrolledEvent(xOffset, yOffset))
            }
        }

        centerWindow()
        glfwShowWindow(handle)
    }

    private fun centerWindow() {
        val vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor()) ?: return
        glfwSetWindowPos(
            handle,
            (vidMode.width() - config.resolution.width) / 2,
            (vidMode.height() - config.resolution.height) / 2
        )
    }

    fun shouldClose(): Boolean = glfwWindowShouldClose(handle)
    fun pollEvents() = glfwPollEvents()

    fun shutdown() = Profiler.profile("Window Shutdown") {
        if (handle != NULL) {
            glfwFreeCallbacks(handle)
            glfwDestroyWindow(handle)
            handle = NULL
            logger.info("Window destroyed")
        }
    }
}