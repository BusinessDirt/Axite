package github.businessdirt.axite.vanadium.platform

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.VanadiumConfig
import github.businessdirt.axite.vanadium.core.events.*
import github.businessdirt.axite.vanadium.core.profiling.Profiler
import github.businessdirt.axite.vanadium.core.utils.BoxCharset
import github.businessdirt.axite.vanadium.core.utils.boxedString
import github.businessdirt.axite.vanadium.core.utils.log
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.MarkerManager
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.system.MemoryUtil.NULL
import kotlin.properties.Delegates

data class WindowData(
    var width: Int,
    var height: Int,
    var framebufferWidth: Int,
    var framebufferHeight: Int,
    val monitorName: String,
    val refreshRate: Int,
    val isResizable: Boolean,
    val isDecorated: Boolean
) {
    val aspectRatio: Float
        get() = framebufferWidth.toFloat() / framebufferHeight.toFloat()

    val contentScale: Float
        get() = framebufferWidth.toFloat() / width.toFloat()
}

class Window(private val config: VanadiumConfig) {
    private val logger: Logger = LogManager.getLogger(Window::class.java)


    lateinit var data: WindowData
    var handle by Delegates.notNull<Long>()

    /**
     * Initializes the window on the current thread.
     * MUST be called from the main thread.
     */
    fun initialize() = Profiler.profile("Window Initialization") {
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

        data = handle.getWindowData()
        boxedString(boxCharset = BoxCharset.ROUNDED, title = "GLFW Initialized") {
            appendLine("Screen Resolution: ${data.width}x${data.height}")
            appendLine("Actual Framebuffer: ${data.framebufferWidth}x${data.framebufferHeight}")
            appendLine("Content Scale: ${"%.2f".format(data.contentScale)}x")
            appendLine("Monitor: ${data.monitorName} @ ${data.refreshRate}Hz")
            appendLine("Resizable: ${data.isResizable}, Decorated: ${data.isDecorated}")
        }.log(logger, Level.DEBUG)

        Profiler.profile("Event Callbacks Setup") {
            glfwSetErrorCallback { error, description ->
                val message = GLFWErrorCallback.getDescription(description)
                logger.atError().withMarker(MarkerManager.getMarker("GLFW")).log("[$error] $message")
            }

            // --- Window / App Events ---
            glfwSetWindowSizeCallback(handle) { _, w, h ->
                data.width = w
                data.height = h
                Vanadium.onEvent(WindowResizedEvent(w, h))
            }

            glfwSetFramebufferSizeCallback(handle) { _, w, h ->
                data.framebufferWidth = w
                data.framebufferHeight = h
                Vanadium.onEvent(FramebufferResizedEvent(w, h))
            }

            glfwSetWindowCloseCallback(handle) { _ ->
                val event = WindowClosedEvent()
                Vanadium.onEvent(event)
                if (event.isCancelled) {
                    glfwSetWindowShouldClose(handle, false)
                }
            }

            glfwSetWindowFocusCallback(handle) { _, focused ->
                Vanadium.onEvent(WindowFocusEvent(focused))
            }

            glfwSetWindowPosCallback(handle) { _, x, y ->
                Vanadium.onEvent(WindowMovedEvent(x, y))
            }

            // --- Keyboard Events ---
            glfwSetKeyCallback(handle) { _, key, _, action, _ ->
                when (action) {
                    GLFW_PRESS -> Vanadium.onEvent(KeyPressedEvent(key, 0))
                    GLFW_REPEAT -> Vanadium.onEvent(KeyPressedEvent(key, 1))
                    GLFW_RELEASE -> Vanadium.onEvent(KeyReleasedEvent(key))
                }
            }

            glfwSetCharCallback(handle) { _, codepoint ->
                Vanadium.onEvent(KeyTypedEvent(codepoint.toChar()))
            }

            // --- Mouse Events ---
            glfwSetCursorPosCallback(handle) { _, x, y ->
                Vanadium.onEvent(MouseMovedEvent(x, y))
            }

            glfwSetMouseButtonCallback(handle) { _, button, action, _ ->
                when (action) {
                    GLFW_PRESS -> Vanadium.onEvent(MouseButtonPressedEvent(button))
                    GLFW_RELEASE -> Vanadium.onEvent(MouseButtonReleasedEvent(button))
                }
            }

            glfwSetScrollCallback(handle) { _, xOffset, yOffset ->
                Vanadium.onEvent(MouseScrolledEvent(xOffset, yOffset))
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
        }
    }
}

fun Long.getWindowData(): WindowData = memoryStack { stack ->
    val w = stack.mallocInt(1)
    val h = stack.mallocInt(1)
    val fw = stack.mallocInt(1)
    val fh = stack.mallocInt(1)

    glfwGetWindowSize(this, w, h)
    glfwGetFramebufferSize(this, fw, fh)

    val monitor = glfwGetPrimaryMonitor()
    val videoMode = glfwGetVideoMode(monitor)

    WindowData(
        width = w[0],
        height = h[0],
        framebufferWidth = fw[0],
        framebufferHeight = fh[0],
        monitorName = glfwGetMonitorName(monitor) ?: "Generic",
        refreshRate = videoMode?.refreshRate() ?: 0,
        isResizable = glfwGetWindowAttrib(this, GLFW_RESIZABLE) == GLFW_TRUE,
        isDecorated = glfwGetWindowAttrib(this, GLFW_DECORATED) == GLFW_TRUE
    )
}
