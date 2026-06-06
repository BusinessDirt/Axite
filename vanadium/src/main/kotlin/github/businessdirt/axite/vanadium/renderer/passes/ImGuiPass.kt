package github.businessdirt.axite.vanadium.renderer.passes

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.assets.loaders.copyImage
import github.businessdirt.axite.vanadium.assets.loaders.createStagingBuffer
import github.businessdirt.axite.vanadium.assets.metadata.TextureMetadata
import github.businessdirt.axite.vanadium.assets.types.Shader
import github.businessdirt.axite.vanadium.assets.types.Texture
import github.businessdirt.axite.vanadium.core.events.*
import github.businessdirt.axite.vanadium.core.profiling.Profiler
import github.businessdirt.axite.vanadium.core.utils.imageBarrier
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.renderer.graph.RenderGraphBuilder
import github.businessdirt.axite.vanadium.renderer.graph.RenderResourceNames
import github.businessdirt.axite.vanadium.vulkan.commands.*
import github.businessdirt.axite.vanadium.vulkan.descriptors.DescriptorPool
import github.businessdirt.axite.vanadium.vulkan.descriptors.DescriptorSet
import github.businessdirt.axite.vanadium.vulkan.pipeline.GraphicsPipeline
import github.businessdirt.axite.vanadium.vulkan.resources.Buffer
import github.businessdirt.axite.vanadium.vulkan.resources.Image
import github.businessdirt.axite.vanadium.vulkan.resources.ImageView
import github.businessdirt.axite.vanadium.vulkan.resources.Sampler
import imgui.ImGui
import imgui.ImVec4
import imgui.flag.ImGuiKey
import imgui.type.ImInt
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkRect2D
import org.lwjgl.vulkan.VkViewport

object ImGuiPass : RenderPass() {

    const val VERTEX_SHADER_PATH = "src/main/resources/shaders/imgui.vert.glsl"
    const val FRAGMENT_SHADER_PATH = "src/main/resources/shaders/imgui.frag.glsl"

    private var graphicsPipeline: GraphicsPipeline? = null
    private var descriptorPool: DescriptorPool? = null

    private var vertexBuffers = mutableListOf<Buffer>()
    private var indexBuffers = mutableListOf<Buffer>()

    private var fontTexture: Texture? = null
    private var fontDescriptorSet: DescriptorSet? = null

    private val guiTexturesMap = mutableMapOf<Long, Long>()

    override suspend fun onInitialize(): Unit = Profiler.profile("ImGuiPass Initialization") {
        val vertexShader = Vanadium.assets.load<Shader>(VERTEX_SHADER_PATH)
        val fragmentShader = Vanadium.assets.load<Shader>(FRAGMENT_SHADER_PATH)

        graphicsPipeline = GraphicsPipeline {
            vertexShader(vertexShader)
            fragmentShader(fragmentShader)
            this.colorFormat = Vanadium.context.surface.surfaceFormat.imageFormat
            this.depthFormat = VK_FORMAT_UNDEFINED
            this.enableBlend = true
        }

        val frames = Vanadium.context.maxFramesInFlight
        descriptorPool = DescriptorPool(Vanadium.context.device.handle, frames * 10, listOf(
            DescriptorPool.PoolSize(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, frames * 10)
        ))

        vertexBuffers = MutableList(frames) {
            Buffer(Vanadium.context.device.handle, Vanadium.context.physicalDevice, 1024, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT)
        }

        indexBuffers = MutableList(frames) {
            Buffer(Vanadium.context.device.handle, Vanadium.context.physicalDevice, 1024, VK_BUFFER_USAGE_INDEX_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT)
        }

        initImGui()
    }

    private fun initImGui() {
        ImGui.createContext()

        val io = ImGui.getIO()
        io.iniFilename = null
        val window = Vanadium.window
        io.setDisplaySize(window.data.width.toFloat(), window.data.height.toFloat())
        io.setDisplayFramebufferScale(window.data.contentScale, window.data.contentScale)

        val fontTextureWidth = ImInt()
        val fontTextureHeight = ImInt()
        val fontTextureBuffer = io.fonts.getTexDataAsRGBA32(fontTextureWidth, fontTextureHeight)

        val width = fontTextureWidth.get()
        val height = fontTextureHeight.get()
        val stagingBuffer = fontTextureBuffer.createStagingBuffer(width, height)

        val image = Image(Vanadium.context.device.handle, Vanadium.context.physicalDevice) {
            this.width = width
            this.height = height
            this.format = VK_FORMAT_R8G8B8A8_SRGB
            this.usage = VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT
            this.mipLevels = 1
        }

        Vanadium.context.graphicsQueue.execute {
            memoryStack { stack ->
                stack.imageBarrier(
                    this.handle, image.handle,
                    VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_2_TRANSFER_BIT,
                    0, VK_ACCESS_2_TRANSFER_WRITE_BIT,
                    VK_IMAGE_ASPECT_COLOR_BIT
                )

                this.copyImage(stack, width, height, stagingBuffer, image)

                stack.imageBarrier(
                    this.handle, image.handle,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                    VK_PIPELINE_STAGE_2_TRANSFER_BIT, VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT,
                    VK_ACCESS_2_TRANSFER_WRITE_BIT, VK_ACCESS_2_SHADER_READ_BIT,
                    VK_IMAGE_ASPECT_COLOR_BIT
                )
            }
        }

        stagingBuffer.close()

        val view = ImageView(Vanadium.context.device.handle, image.handle) {
            this.format = VK_FORMAT_R8G8B8A8_SRGB
            this.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT
        }

        val sampler = Sampler(Vanadium.context.device.handle) {
            this.magFilter = VK_FILTER_LINEAR
            this.minFilter = VK_FILTER_LINEAR
            this.addressModeU = VK_SAMPLER_ADDRESS_MODE_REPEAT
            this.addressModeV = VK_SAMPLER_ADDRESS_MODE_REPEAT
            this.addressModeW = VK_SAMPLER_ADDRESS_MODE_REPEAT
        }

        fontTexture = Texture("ImGuiFont", "imgui-font-uuid", TextureMetadata(), image, view, sampler)
        
        val layout = graphicsPipeline!!.layout.descriptorSetLayouts[0]
        fontDescriptorSet = DescriptorSet(Vanadium.context.device.handle, descriptorPool!!, layout).apply {
            updateImage(0, view.handle, sampler.handle)
        }

        io.fonts.setTexID(fontDescriptorSet!!.handle)
    }

    fun newFrame() {
        val io = ImGui.getIO()
        val window = Vanadium.window
        io.setDisplaySize(window.data.width.toFloat(), window.data.height.toFloat())
        io.setDisplayFramebufferScale(window.data.contentScale, window.data.contentScale)
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
        builder.pass("ImGuiPass") {
            read(colorOutput)
            writes(colorOutput)
            pipeline { commandBuffer, _ ->
                render(commandBuffer)
            }
        }

        block()
        ImGui.render()
    }

    private fun render(commandBuffer: CommandBuffer) {
        val drawData = ImGui.getDrawData() ?: return
        if (drawData.ptr == 0L) return

        val frameIndex = Vanadium.context.currentFrameIndex
        updateBuffers(drawData, frameIndex)

        val pipeline = graphicsPipeline ?: return
        pipeline.bind(commandBuffer)

        val extent = Vanadium.context.swapchain.extent
        val width = extent.width()
        val height = extent.height()

        memoryStack { stack ->
            val viewport = VkViewport.calloc(1, stack)
                .x(0f).y(0f)
                .width(width.toFloat()).height(height.toFloat())
                .minDepth(0f).maxDepth(1f)
            vkCmdSetViewport(commandBuffer.handle, 0, viewport)

            val scissor = VkRect2D.calloc(1, stack)
            scissor.offset { it.x(0).y(0) }
            scissor.extent { it.width(width).height(height) }
            vkCmdSetScissor(commandBuffer.handle, 0, scissor)

            commandBuffer.bindVertexBuffer(vertexBuffers[frameIndex].handle)
            commandBuffer.bindIndexBuffer(indexBuffers[frameIndex].handle, indexType = VK_INDEX_TYPE_UINT16)

            val io = ImGui.getIO()
            val pushConstants = stack.malloc(8)
            pushConstants.putFloat(0, 2.0f / io.displaySizeX)
            pushConstants.putFloat(4, 2.0f / io.displaySizeY)
            commandBuffer.pushConstants(pipeline.layout.handle, VK_SHADER_STAGE_VERTEX_BIT, pushConstants)

            val imVec4 = ImVec4()
            val scale = Vanadium.window.data.contentScale
            var offsetIdx = 0
            var offsetVtx = 0
            
            for (i in 0 until drawData.cmdListsCount) {
                for (j in 0 until drawData.getCmdListCmdBufferSize(i)) {
                    val texID = drawData.getCmdListCmdBufferTextureId(i, j)
                    val descriptorSet = guiTexturesMap[texID] ?: texID
                    
                    commandBuffer.bindDescriptorSets(pipeline.layout.handle, longArrayOf(descriptorSet))

                    drawData.getCmdListCmdBufferClipRect(imVec4, i, j)
                    scissor.offset { 
                        it.x((imVec4.x * scale).toInt().coerceAtLeast(0))
                        it.y((imVec4.y * scale).toInt().coerceAtLeast(0))
                    }
                    scissor.extent { 
                        it.width(((imVec4.z - imVec4.x) * scale).toInt())
                        it.height(((imVec4.w - imVec4.y) * scale).toInt())
                    }
                    vkCmdSetScissor(commandBuffer.handle, 0, scissor)

                    val count = drawData.getCmdListCmdBufferElemCount(i, j)
                    vkCmdDrawIndexed(
                        commandBuffer.handle, count, 1,
                        offsetIdx + drawData.getCmdListCmdBufferIdxOffset(i, j),
                        offsetVtx + drawData.getCmdListCmdBufferVtxOffset(i, j), 0
                    )
                }
                offsetIdx += drawData.getCmdListIdxBufferSize(i)
                offsetVtx += drawData.getCmdListVtxBufferSize(i)
            }
        }
    }

    private fun updateBuffers(drawData: imgui.ImDrawData, frameIndex: Int) {
        val totalVtxSize = drawData.totalVtxCount * 20L // ImDrawVert size: pos(8) + uv(8) + col(4)
        val totalIdxSize = drawData.totalIdxCount * 2L // ImDrawIdx size: 2 bytes (ushort)

        if (totalVtxSize == 0L || totalIdxSize == 0L) return

        if (vertexBuffers[frameIndex].size < totalVtxSize) {
            vertexBuffers[frameIndex].close()
            vertexBuffers[frameIndex] = Buffer(Vanadium.context.device.handle, Vanadium.context.physicalDevice, totalVtxSize, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT)
        }

        if (indexBuffers[frameIndex].size < totalIdxSize) {
            indexBuffers[frameIndex].close()
            indexBuffers[frameIndex] = Buffer(Vanadium.context.device.handle, Vanadium.context.physicalDevice, totalIdxSize, VK_BUFFER_USAGE_INDEX_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT)
        }

        val vtxPtr = vertexBuffers[frameIndex].map()
        val idxPtr = indexBuffers[frameIndex].map()

        val vtxBuffer = MemoryUtil.memByteBuffer(vtxPtr, totalVtxSize.toInt())
        val idxBuffer = MemoryUtil.memByteBuffer(idxPtr, totalIdxSize.toInt())

        for (i in 0 until drawData.cmdListsCount) {
            vtxBuffer.put(drawData.getCmdListVtxBufferData(i))
            idxBuffer.put(drawData.getCmdListIdxBufferData(i))
        }

        vertexBuffers[frameIndex].unmap()
        indexBuffers[frameIndex].unmap()
    }

    override fun onShutdown() = Profiler.profile("ImGuiPass Shutdown") {
        Vanadium.assets.unload(VERTEX_SHADER_PATH)
        Vanadium.assets.unload(FRAGMENT_SHADER_PATH)

        fontTexture?.close()
        fontDescriptorSet?.close()

        indexBuffers.forEach { it.close() }
        vertexBuffers.forEach { it.close() }

        descriptorPool?.close()
        descriptorPool = null

        graphicsPipeline?.close()
        graphicsPipeline = null

        ImGui.destroyContext()
    }

    override fun onImGuiRender() {

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