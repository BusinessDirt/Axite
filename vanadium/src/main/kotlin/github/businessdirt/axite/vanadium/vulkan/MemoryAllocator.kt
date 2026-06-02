package github.businessdirt.axite.vanadium.vulkan

import github.businessdirt.axite.vanadium.core.utils.createPointer
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.device.Device
import github.businessdirt.axite.vanadium.vulkan.device.PhysicalDevice
import org.lwjgl.util.vma.Vma.vmaCreateAllocator
import org.lwjgl.util.vma.Vma.vmaDestroyAllocator
import org.lwjgl.util.vma.VmaAllocatorCreateInfo
import org.lwjgl.util.vma.VmaVulkanFunctions
import org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3

class MemoryAllocator(
    instance: Instance,
    physicalDevice: PhysicalDevice,
    device: Device,
) : Handle<Long>() {


    override val handle: Long = memoryStack { stack ->
        stack.createPointer({ "Failed to create VMA allocator" }) { pAllocator ->
            val vmaVulkanFunctions = VmaVulkanFunctions.calloc(stack).set(instance.handle, device.handle)
            val createInfo = VmaAllocatorCreateInfo.calloc(stack)
                .instance(instance.handle)
                .vulkanApiVersion(VK_API_VERSION_1_3)
                .device(device.handle)
                .physicalDevice(physicalDevice.handle)
                .pVulkanFunctions(vmaVulkanFunctions)

            vmaCreateAllocator(createInfo, pAllocator)
        }
    }

    override fun destroy() = vmaDestroyAllocator(handle)

}