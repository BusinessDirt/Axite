package github.businessdirt.axite.vanadium.vulkan.device

import github.businessdirt.axite.vanadium.core.utils.Platform
import github.businessdirt.axite.vanadium.core.utils.PlatformUtils
import github.businessdirt.axite.vanadium.core.utils.createPointer
import github.businessdirt.axite.vanadium.core.utils.debugGrid
import github.businessdirt.axite.vanadium.core.utils.memoryStack
import github.businessdirt.axite.vanadium.vulkan.Handle
import org.apache.logging.log4j.LogManager
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME
import org.lwjgl.vulkan.VK13.*

class Device(
    physicalDevice: PhysicalDevice
) : Handle<VkDevice>() {

    override val handle: VkDevice = memoryStack { stack ->
        val reqExtensions: PointerBuffer = physicalDevice.createReqExtensions(stack)
        val queueProps = physicalDevice.queueFamilyProperties
        val numQueueFamilies: Int = queueProps.capacity()
        val queueCreationInfos = VkDeviceQueueCreateInfo.calloc(numQueueFamilies, stack)

        (0 until numQueueFamilies).forEach { i ->
            val queueCount = queueProps[i].queueCount()
            val priorities = stack.callocFloat(queueCount)

            queueCreationInfos[i].`sType$Default`()
                .queueFamilyIndex(i)
                .pQueuePriorities(priorities)
        }

        val features13 = VkPhysicalDeviceVulkan13Features.calloc(stack).`sType$Default`()
            .dynamicRendering(true)
            .synchronization2(true)

        val features2 = VkPhysicalDeviceFeatures2.calloc(stack).`sType$Default`()
            .pNext(features13.address())

        val deviceCreateInfo: VkDeviceCreateInfo = VkDeviceCreateInfo.calloc(stack).`sType$Default`()
            .pNext(features2.address())
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

    LogManager.getLogger(Device::class.java).debugGrid(
        "Enabling Logical Device Extensions [${extensionsToEnable.size}]",
        extensionsToEnable
    ) { name ->
        name.split("_").getOrNull(1) ?: "CORE"
    }

    return stack.mallocPointer(extensionsToEnable.size).apply {
        extensionsToEnable.forEach { put(stack.ASCII(it)) }
        flip()
    }
}