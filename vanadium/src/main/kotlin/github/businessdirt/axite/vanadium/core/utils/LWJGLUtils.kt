package github.businessdirt.axite.vanadium.core.utils

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import java.nio.IntBuffer
import java.nio.LongBuffer

fun <T> memoryStack(block: (MemoryStack) -> T): T =
    MemoryStack.stackPush().use { block(it) }

/**
 * Executes a Vulkan creation function that returns a single Long handle.
 * @param errorMessage The error message to throw if the vkCheck fails.
 * @param block A lambda that takes the LongBuffer and returns the VkResult.
 */
inline fun MemoryStack.createHandle(
    errorMessage: () -> String,
    block: (LongBuffer) -> Int
): Long {
    val longBuffer = this.mallocLong(1)
    vkCheck(block(longBuffer), errorMessage)
    return longBuffer[0]
}

inline fun MemoryStack.createPointer(
    errorMessage: () -> String,
    block: (PointerBuffer) -> Int
): Long {
    val pp = mallocPointer(1)
    vkCheck(block(pp), errorMessage)
    return pp[0]
}

inline fun MemoryStack.getPointer(
    block: (PointerBuffer) -> Unit
): Long {
    val pp = mallocPointer(1)
    block(pp)
    return pp[0]
}

/**
 * Allocates a temporary IntBuffer, executes a block, and returns the first value.
 */
inline fun MemoryStack.getInt(block: (IntBuffer) -> Unit): Int {
    val ib = this.mallocInt(1)
    block(ib)
    return ib[0]
}