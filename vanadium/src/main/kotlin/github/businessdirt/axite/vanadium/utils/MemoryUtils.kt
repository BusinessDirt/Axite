package github.businessdirt.axite.vanadium.utils

import org.lwjgl.system.MemoryStack

fun <T> memoryStack(block: (MemoryStack) -> T): T =
    MemoryStack.stackPush().use { block(it) }