package github.businessdirt.axite.vanadium.renderer

import github.businessdirt.axite.vanadium.Vanadium
import github.businessdirt.axite.vanadium.assets.types.Shader
import github.businessdirt.axite.vanadium.core.profiling.Profiler
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.Context
import github.businessdirt.axite.vanadium.vulkan.commands.CommandBuffer
import github.businessdirt.axite.vanadium.vulkan.commands.bindDescriptorSets
import github.businessdirt.axite.vanadium.vulkan.commands.draw
import github.businessdirt.axite.vanadium.vulkan.descriptors.DescriptorPool
import github.businessdirt.axite.vanadium.vulkan.descriptors.DescriptorSet
import github.businessdirt.axite.vanadium.vulkan.pipeline.GraphicsPipeline
import github.businessdirt.axite.vanadium.vulkan.resources.Attachment
import github.businessdirt.axite.vanadium.vulkan.resources.Buffer
import github.businessdirt.axite.vanadium.vulkan.resources.Sampler
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK13.*

class PostProcessingRenderer(val context: Context) {

    companion object {
        const val VERTEX_SHADER_PATH = "src/main/resources/shaders/post.vert.glsl"
        const val FRAGMENT_SHADER_PATH = "src/main/resources/shaders/post.frag.glsl"
    }

    private var graphicsPipeline: GraphicsPipeline? = null
    private var descriptorPool: DescriptorPool? = null
    private var imageSets: Array<DescriptorSet>? = null
    private var screenSizeSets: Array<DescriptorSet>? = null
    private var sampler: Sampler? = null
    private var screenSizeBuffers: Array<Buffer>? = null

    private var lastInputViews: LongArray? = null

    var effectType: Int = 1
        set(value) {
            if (field != value) {
                field = value
                recreatePipeline()
            }
        }

    var useFxaa: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                recreatePipeline()
            }
        }

    suspend fun initialize() = Profiler.profile("PostProcessingRenderer Initialization") {
        val vertexShader = Vanadium.assets.load<Shader>(VERTEX_SHADER_PATH)
        val fragmentShader = Vanadium.assets.load<Shader>(FRAGMENT_SHADER_PATH)

        sampler = Sampler(context.device.handle) {
            magFilter = VK_FILTER_LINEAR
            minFilter = VK_FILTER_LINEAR
            addressModeU = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE
            addressModeV = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE
        }

        descriptorPool = DescriptorPool(
            context.device.handle, context.maxFramesInFlight * 2, listOf(
                DescriptorPool.PoolSize(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, context.maxFramesInFlight),
                DescriptorPool.PoolSize(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, context.maxFramesInFlight)
            )
        )

        screenSizeBuffers = Array(context.maxFramesInFlight) {
            Buffer(
                context.device.handle,
                context.physicalDevice,
                8, // vec2 (float, float)
                VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
            )
        }

        lastInputViews = LongArray(context.maxFramesInFlight) { 0L }

        recreatePipeline(vertexShader, fragmentShader)
    }

    private fun recreatePipeline(
        vertexShader: Shader? = graphicsPipeline?.configuration?.vertexShader,
        fragmentShader: Shader? = graphicsPipeline?.configuration?.fragmentShader
    ) {
        val oldPipeline = graphicsPipeline
        graphicsPipeline = GraphicsPipeline {
            this.vertexShader(vertexShader!!)
            this.fragmentShader(fragmentShader!!)
            specializationConstant("EFFECT_TYPE", effectType)
            specializationConstant("USE_FXAA", if (useFxaa) 1 else 0)
        }
        oldPipeline?.close()

        // Recreate descriptor sets as they are tied to the pipeline layout
        if (descriptorPool != null) {
            imageSets?.forEach { it.close() }
            screenSizeSets?.forEach { it.close() }

            graphicsPipeline?.layout?.descriptorSetLayouts?.let { layouts ->
                imageSets = Array(context.maxFramesInFlight) { i ->
                    DescriptorSet(context.device.handle, descriptorPool!!, layouts[0])
                }
                screenSizeSets = Array(context.maxFramesInFlight) { i ->
                    DescriptorSet(context.device.handle, descriptorPool!!, layouts[1]).also { set ->
                        set.updateBuffer(0, screenSizeBuffers!![i].handle, 8)
                    }
                }
            }

            // Reset view tracking
            lastInputViews?.fill(0L)
        }
    }

    fun render(commandBuffer: CommandBuffer, input: Attachment) {
        val pipeline = graphicsPipeline ?: return
        val frameIndex = context.currentFrameIndex
        val imgSet = imageSets?.get(frameIndex) ?: return
        val sizeSet = screenSizeSets?.get(frameIndex) ?: return
        val sizeBuffer = screenSizeBuffers?.get(frameIndex) ?: return

        // Update screen size
        val map = sizeBuffer.map()
        val buffer = MemoryUtil.memByteBuffer(map, 8)
        buffer.putFloat(input.width.toFloat())
        buffer.putFloat(input.height.toFloat())
        sizeBuffer.unmap()

        // Only update if view changed
        if (lastInputViews!![frameIndex] != input.imageView.handle) {
            imgSet.updateImage(0, input.imageView.handle, sampler!!.handle)
            lastInputViews!![frameIndex] = input.imageView.handle
        }

        pipeline.bind(commandBuffer)
        commandBuffer.bindDescriptorSets(pipeline.layout.handle, longArrayOf(imgSet.handle, sizeSet.handle))
        commandBuffer.draw(3)
    }

    fun shutdown() {
        imageSets?.forEach { it.close() }
        screenSizeSets?.forEach { it.close() }
        descriptorPool?.close()
        sampler?.close()
        screenSizeBuffers?.forEach { it.close() }
        graphicsPipeline?.close()

        Vanadium.assets.unload(VERTEX_SHADER_PATH)
        Vanadium.assets.unload(FRAGMENT_SHADER_PATH)
    }
}
