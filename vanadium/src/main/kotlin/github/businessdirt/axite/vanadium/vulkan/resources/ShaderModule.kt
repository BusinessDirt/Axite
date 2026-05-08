package github.businessdirt.axite.vanadium.vulkan.resources

import github.businessdirt.axite.vanadium.core.utils.createHandle
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.Handle
import org.lwjgl.vulkan.VK13.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkShaderModuleCreateInfo
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

class ShaderModule(
    private val device: VkDevice,
    val stage: Int,
    path: String
) : Handle<Long>() {

    override val handle: Long

    init {
        // Use a FileChannel to map the file.
        // This is zero-copy and ensures we don't bloat the JVM heap.
        val channel = FileChannel.open(Paths.get(path), StandardOpenOption.READ)
        val buffer: ByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())

        // SPIR-V requirement check: Size must be a multiple of 4
        if (buffer.remaining() % 4 != 0) {
            channel.close()
            throw IllegalArgumentException("SPIR-V shader size is not a multiple of 4: $path")
        }

        handle = memoryStack { stack ->
            val createInfo = VkShaderModuleCreateInfo.calloc(stack).`sType$Default`().pCode(buffer)
            stack.createHandle({ "Failed to create shader module: $path" }) {
                vkCreateShaderModule(device, createInfo, null, it)
            }
        }

        channel.close() // The mapping remains valid until the buffer is GC'd or unmapped
    }

    override fun destroy() = vkDestroyShaderModule(device, handle, null)
}