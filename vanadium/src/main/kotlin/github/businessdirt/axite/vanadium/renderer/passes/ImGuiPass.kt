package github.businessdirt.axite.vanadium.renderer.passes

import github.businessdirt.axite.vanadium.core.events.*
import github.businessdirt.axite.vanadium.core.profiling.Profiler
import github.businessdirt.axite.vanadium.renderer.graph.RenderGraphBuilder
import github.businessdirt.axite.vanadium.renderer.graph.RenderResourceNames
import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer
import imgui.ImGui
import imgui.flag.ImGuiKey
import org.lwjgl.glfw.GLFW.*

object ImGuiPass : RenderPass() {

    const val DESC_ID_TEXT = "IM_GUI_DESC_ID_TEXT"
    const val VERTEX_SHADER_PATH = "src/main/resources/shaders/imgui.vert.glsl"
    const val FRAGMENT_SHADER_PATH = "src/main/resources/shaders/imgui.frag.glsl"

    override suspend fun onInitialize(): Unit = Profiler.profile("ImGuiPass Initialization") {

    }

    fun newFrame() {
        ImGui.newFrame()
    }

    fun onEvent(event: Event) {
        if (!isInitialized) return

        val io = ImGui.getIO()
        val dispatcher = EventDispatcher(event)

        dispatcher.dispatch<KeyPressedEvent> {
            io.addKeyEvent(getImKey(it.keyCode), true)
            if (io.wantCaptureKeyboard) it.isHandled = true
        }

        dispatcher.dispatch<KeyReleasedEvent> {
            io.addKeyEvent(getImKey(it.keyCode), false)
            if (io.wantCaptureKeyboard) it.isHandled = true
        }

        dispatcher.dispatch<KeyTypedEvent> {
            io.addInputCharacter(it.char.code)
            if (io.wantCaptureKeyboard) it.isHandled = true
        }

        dispatcher.dispatch<MouseMovedEvent> {
            io.setMousePos(it.x.toFloat(), it.y.toFloat())
            if (io.wantCaptureMouse) it.isHandled = true
        }

        dispatcher.dispatch<MouseButtonPressedEvent> {
            io.setMouseDown(it.button, true)
            if (io.wantCaptureMouse) it.isHandled = true
        }

        dispatcher.dispatch<MouseButtonReleasedEvent> {
            io.setMouseDown(it.button, false)
            if (io.wantCaptureMouse) it.isHandled = true
        }

        dispatcher.dispatch<MouseScrolledEvent> {
            io.mouseWheelH = it.xOffset.toFloat()
            io.mouseWheel = it.yOffset.toFloat()
            if (io.wantCaptureMouse) it.isHandled = true
        }
    }

    fun record(builder: RenderGraphBuilder, colorOutput: String = RenderResourceNames.BACK_BUFFER, block: () -> Unit) {
        block()
        builder.pass("ImGuiPass") {
            read(colorOutput)
            writes(colorOutput)
            pipeline { commandBuffer, _ ->
                render(commandBuffer)
            }
        }
    }

    private fun render(commandBuffer: CommandBuffer) {

    }

    override fun onShutdown() = Profiler.profile("ImGuiPass Shutdown") {

    }
}

private fun getImKey(key: Int): Int = when (key) {
    GLFW_KEY_TAB -> ImGuiKey.Tab
    GLFW_KEY_LEFT -> ImGuiKey.LeftArrow
    GLFW_KEY_RIGHT -> ImGuiKey.RightArrow
    GLFW_KEY_UP -> ImGuiKey.UpArrow
    GLFW_KEY_DOWN -> ImGuiKey.DownArrow
    GLFW_KEY_PAGE_UP -> ImGuiKey.PageUp
    GLFW_KEY_PAGE_DOWN -> ImGuiKey.PageDown
    GLFW_KEY_HOME -> ImGuiKey.Home
    GLFW_KEY_END -> ImGuiKey.End
    GLFW_KEY_INSERT -> ImGuiKey.Insert
    GLFW_KEY_DELETE -> ImGuiKey.Delete
    GLFW_KEY_BACKSPACE -> ImGuiKey.Backspace
    GLFW_KEY_SPACE -> ImGuiKey.Space
    GLFW_KEY_ENTER -> ImGuiKey.Enter
    GLFW_KEY_ESCAPE -> ImGuiKey.Escape
    GLFW_KEY_APOSTROPHE -> ImGuiKey.Apostrophe
    GLFW_KEY_COMMA -> ImGuiKey.Comma
    GLFW_KEY_MINUS -> ImGuiKey.Minus
    GLFW_KEY_PERIOD -> ImGuiKey.Period
    GLFW_KEY_SLASH -> ImGuiKey.Slash
    GLFW_KEY_SEMICOLON -> ImGuiKey.Semicolon
    GLFW_KEY_EQUAL -> ImGuiKey.Equal
    GLFW_KEY_LEFT_BRACKET -> ImGuiKey.LeftBracket
    GLFW_KEY_BACKSLASH -> ImGuiKey.Backslash
    GLFW_KEY_RIGHT_BRACKET -> ImGuiKey.RightBracket
    GLFW_KEY_GRAVE_ACCENT -> ImGuiKey.GraveAccent
    GLFW_KEY_CAPS_LOCK -> ImGuiKey.CapsLock
    GLFW_KEY_SCROLL_LOCK -> ImGuiKey.ScrollLock
    GLFW_KEY_NUM_LOCK -> ImGuiKey.NumLock
    GLFW_KEY_PRINT_SCREEN -> ImGuiKey.PrintScreen
    GLFW_KEY_PAUSE -> ImGuiKey.Pause
    GLFW_KEY_KP_0 -> ImGuiKey.Keypad0
    GLFW_KEY_KP_1 -> ImGuiKey.Keypad1
    GLFW_KEY_KP_2 -> ImGuiKey.Keypad2
    GLFW_KEY_KP_3 -> ImGuiKey.Keypad3
    GLFW_KEY_KP_4 -> ImGuiKey.Keypad4
    GLFW_KEY_KP_5 -> ImGuiKey.Keypad5
    GLFW_KEY_KP_6 -> ImGuiKey.Keypad6
    GLFW_KEY_KP_7 -> ImGuiKey.Keypad7
    GLFW_KEY_KP_8 -> ImGuiKey.Keypad8
    GLFW_KEY_KP_9 -> ImGuiKey.Keypad9
    GLFW_KEY_KP_DECIMAL -> ImGuiKey.KeypadDecimal
    GLFW_KEY_KP_DIVIDE -> ImGuiKey.KeypadDivide
    GLFW_KEY_KP_MULTIPLY -> ImGuiKey.KeypadMultiply
    GLFW_KEY_KP_SUBTRACT -> ImGuiKey.KeypadSubtract
    GLFW_KEY_KP_ADD -> ImGuiKey.KeypadAdd
    GLFW_KEY_KP_ENTER -> ImGuiKey.KeypadEnter
    GLFW_KEY_KP_EQUAL -> ImGuiKey.KeypadEqual
    GLFW_KEY_LEFT_SHIFT -> ImGuiKey.LeftShift
    GLFW_KEY_LEFT_CONTROL -> ImGuiKey.LeftCtrl
    GLFW_KEY_LEFT_ALT -> ImGuiKey.LeftAlt
    GLFW_KEY_LEFT_SUPER -> ImGuiKey.LeftSuper
    GLFW_KEY_RIGHT_SHIFT -> ImGuiKey.RightShift
    GLFW_KEY_RIGHT_CONTROL -> ImGuiKey.RightCtrl
    GLFW_KEY_RIGHT_ALT -> ImGuiKey.RightAlt
    GLFW_KEY_RIGHT_SUPER -> ImGuiKey.RightSuper
    GLFW_KEY_MENU -> ImGuiKey.Menu
    GLFW_KEY_0 -> ImGuiKey._0
    GLFW_KEY_1 -> ImGuiKey._1
    GLFW_KEY_2 -> ImGuiKey._2
    GLFW_KEY_3 -> ImGuiKey._3
    GLFW_KEY_4 -> ImGuiKey._4
    GLFW_KEY_5 -> ImGuiKey._5
    GLFW_KEY_6 -> ImGuiKey._6
    GLFW_KEY_7 -> ImGuiKey._7
    GLFW_KEY_8 -> ImGuiKey._8
    GLFW_KEY_9 -> ImGuiKey._9
    GLFW_KEY_A -> ImGuiKey.A
    GLFW_KEY_B -> ImGuiKey.B
    GLFW_KEY_C -> ImGuiKey.C
    GLFW_KEY_D -> ImGuiKey.D
    GLFW_KEY_E -> ImGuiKey.E
    GLFW_KEY_F -> ImGuiKey.F
    GLFW_KEY_G -> ImGuiKey.G
    GLFW_KEY_H -> ImGuiKey.H
    GLFW_KEY_I -> ImGuiKey.I
    GLFW_KEY_J -> ImGuiKey.J
    GLFW_KEY_K -> ImGuiKey.K
    GLFW_KEY_L -> ImGuiKey.L
    GLFW_KEY_M -> ImGuiKey.M
    GLFW_KEY_N -> ImGuiKey.N
    GLFW_KEY_O -> ImGuiKey.O
    GLFW_KEY_P -> ImGuiKey.P
    GLFW_KEY_Q -> ImGuiKey.Q
    GLFW_KEY_R -> ImGuiKey.R
    GLFW_KEY_S -> ImGuiKey.S
    GLFW_KEY_T -> ImGuiKey.T
    GLFW_KEY_U -> ImGuiKey.U
    GLFW_KEY_V -> ImGuiKey.V
    GLFW_KEY_W -> ImGuiKey.W
    GLFW_KEY_X -> ImGuiKey.X
    GLFW_KEY_Y -> ImGuiKey.Y
    GLFW_KEY_Z -> ImGuiKey.Z
    GLFW_KEY_F1 -> ImGuiKey.F1
    GLFW_KEY_F2 -> ImGuiKey.F2
    GLFW_KEY_F3 -> ImGuiKey.F3
    GLFW_KEY_F4 -> ImGuiKey.F4
    GLFW_KEY_F5 -> ImGuiKey.F5
    GLFW_KEY_F6 -> ImGuiKey.F6
    GLFW_KEY_F7 -> ImGuiKey.F7
    GLFW_KEY_F8 -> ImGuiKey.F8
    GLFW_KEY_F9 -> ImGuiKey.F9
    GLFW_KEY_F10 -> ImGuiKey.F10
    GLFW_KEY_F11 -> ImGuiKey.F11
    GLFW_KEY_F12 -> ImGuiKey.F12
    else -> ImGuiKey.None
}