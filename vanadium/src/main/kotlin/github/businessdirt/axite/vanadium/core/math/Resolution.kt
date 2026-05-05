package github.businessdirt.axite.vanadium.core.math

data class Resolution(
    val width: Int,
    val height: Int,
) {
    val isValid: Boolean
        get() = width > 0 && height > 0

    val isInvalid: Boolean
        get() = !isValid

    companion object {
        val EMPTY = Resolution(0, 0)
    }
}