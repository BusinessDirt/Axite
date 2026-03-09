package github.businessdirt.axite.vanadium

import org.lwjgl.glfw.GLFW

object Vanadium {

    val time: Double
        get() = GLFW.glfwGetTime()
}