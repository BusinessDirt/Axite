package github.businessdirt.axite.vanadium.utils

import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.Struct
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.nio.DoubleBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.LongBuffer

fun Struct<*>.extractMembers(): List<Method> = this::class.java.declaredMethods
    .asSequence()
    .filter { it.parameterCount == 0 }
    .filter { !Modifier.isStatic(it.modifiers) }
    .filter { !it.name.contains("$") }
    .filter { it.name !in setOf("address", "capacity", "sizeof", "clear", "free", "close") }
    .filter { it.returnType.name != "java.nio.ByteBuffer" }
    .sortedBy { it.name }
    .toList()

fun Struct<*>.extractValue(method: Method): Any?  = try {
    method.invoke(this)
} catch (_: Exception) {
    null
}

/**
 * Recursively prints LWJGL Struct properties (limits, API versions, etc.)
 */
fun Struct<*>.debugTree(
    name: String,
    indent: String = "",
    isLast: Boolean = true,
    visitedAddresses: MutableSet<Long> = mutableSetOf(),
    consumer: (String) -> Unit = { println(it) }
) {
    val marker = if (indent.isEmpty()) "" else if (isLast) "└── " else "├── "
    val currentPrefix = "$indent$marker"
    val nextIndent = indent + if (indent.isEmpty()) "" else if (isLast) "    " else "│   "

    // If we have already parsed this exact C-memory address in this chain, STOP!
    if (!visitedAddresses.add(this.address())) {
        consumer("$currentPrefix$name [Circular Reference -> ${this::class.simpleName}]")
        return
    }

    consumer("$currentPrefix$name")

    // Grab all getter methods
    val methods: List<Method> = extractMembers()
    methods.forEachIndexed { index, method ->
        val isLastMethod = index == methods.size - 1
        val childMarker = if (isLastMethod) "└── " else "├── "

        val result: Any = extractValue(method) ?: return@forEachIndexed
        when (result) {
            is Struct<*> -> result.debugTree(method.name, nextIndent, isLastMethod, visitedAddresses, consumer)
            else -> {
                val value = when (result) {
                    is FloatBuffer -> contentToString(result.limit()) { result.get(it).toString() }
                    is DoubleBuffer -> contentToString(result.limit()) { result.get(it).toString() }
                    is IntBuffer -> contentToString(result.limit()) { result.get(it).toString() }
                    is LongBuffer -> contentToString(result.limit()) { result.get(it).toString() }
                    else -> result.toString()
                }

                consumer("$nextIndent$childMarker ${method.name}=$value")
            }
        }
    }
}

private fun contentToString(limit: Int, supplier: (Int) -> String): String =
    (0 until limit).joinToString(prefix = "[", postfix = "]") { supplier(it) }

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