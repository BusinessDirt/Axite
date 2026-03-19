package github.businessdirt.axite.vanadium.platform.vulkan

import github.businessdirt.axite.vanadium.utils.Platform
import github.businessdirt.axite.vanadium.utils.PlatformUtils
import github.businessdirt.axite.vanadium.utils.createPointer
import github.businessdirt.axite.vanadium.utils.memoryStack
import github.businessdirt.axite.vanadium.utils.vkCheck
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME
import org.lwjgl.vulkan.VK13.vkCreateDevice
import org.lwjgl.vulkan.VK13.vkDestroyDevice
import org.lwjgl.vulkan.VK13.vkDeviceWaitIdle
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkDeviceCreateInfo
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo
import kotlin.collections.forEach


class Device(
    physicalDevice: PhysicalDevice
) : VulkanHandle<VkDevice>() {

    override val handle: VkDevice = memoryStack { stack ->
        val reqExtensions: PointerBuffer = physicalDevice.createReqExtensions(stack)
        val queueProps = physicalDevice.queueFamilyProperties
        val numQueueFamilies: Int = queueProps.capacity()
        val queueCreationInfos = VkDeviceQueueCreateInfo.calloc(numQueueFamilies, stack)

        (0 until numQueueFamilies).forEach { i ->
            val queueCount = queueProps[i].queueCount()
            val priorities = stack.callocFloat(queueCount)

            queueCreationInfos[i]
                .`sType$Default`()
                .queueFamilyIndex(i)
                .pQueuePriorities(priorities)
        }

        val deviceCreateInfo: VkDeviceCreateInfo = VkDeviceCreateInfo.calloc(stack)
            .`sType$Default`()
            .ppEnabledExtensionNames(reqExtensions)
            .pQueueCreateInfos(queueCreationInfos)

        val deviceHandle: Long = stack.createPointer({ "Failed to create logical device" }) { pointerBuffer ->
            vkCreateDevice(physicalDevice.handle, deviceCreateInfo, null, pointerBuffer)
        }

        VkDevice(deviceHandle, physicalDevice.handle, deviceCreateInfo)
    }

    fun waitIdle() = vkDeviceWaitIdle(handle)

    override fun destroy() = vkDestroyDevice(handle, null)
}

private fun PhysicalDevice.createReqExtensions(stack: MemoryStack): PointerBuffer {
    val supportedExtensions = (0 until extensions.capacity()).map { i ->
        extensions[i].extensionNameString()
    }.toSet()

    val usePortability = VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME in supportedExtensions &&
            PlatformUtils.type == Platform.MACOS

    val extensionsToEnable = buildList {
        addAll(PhysicalDevice.REQUIRED_EXTENSIONS)
        if (usePortability) {
            add(VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME)
        }
    }

    return stack.mallocPointer(extensionsToEnable.size).apply {
        extensionsToEnable.forEach { put(stack.ASCII(it)) }
        flip()
    }
}