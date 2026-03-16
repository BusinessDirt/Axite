package github.businessdirt.axite.vanadium.utils

import org.lwjgl.vulkan.VK13.*


enum class Platform(
    val displayName: String,
) {
    WINDOWS("Windows"),
    MACOS("MacOS"),
    LINUX("Linux"),
    OTHER("Other")
}

object PlatformUtils {
    val type: Platform
        get() = with(System.getProperty("os.name", "generic").lowercase()) {
            when {
                contains("mac") || contains("darwin") -> Platform.MACOS
                contains("win") -> Platform.WINDOWS
                contains("nux") -> Platform.LINUX
                else -> Platform.OTHER
            }
        }
}