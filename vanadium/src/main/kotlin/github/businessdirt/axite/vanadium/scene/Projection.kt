package github.businessdirt.axite.vanadium.scene

import org.joml.Matrix4f

class Projection(
    val fov: Float,
    val zNear: Float,
    val zFar: Float,
    val width: Int,
    val height: Int,
) {

    val matrix = Matrix4f()

    init {
        resize(width, height)
    }

    fun resize(width: Int, height: Int) {
        matrix.identity()
        matrix.perspective(fov, width.toFloat() / height.toFloat(), zNear, zFar, true)
    }
}