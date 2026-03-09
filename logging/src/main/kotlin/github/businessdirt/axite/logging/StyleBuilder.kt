package github.businessdirt.axite.logging

/**
 * Builder for Log4j2 styles.
 */
@Suppress("unused")
class StyleBuilder {
    private val styles = mutableListOf<String>()

    fun bold() = apply { styles.add("bold") }
    fun dim() = apply { styles.add("dim") }
    fun italic() = apply { styles.add("italic") }
    fun underline() = apply { styles.add("underline") }
    fun blink() = apply { styles.add("blink") }
    fun reverse() = apply { styles.add("reverse") }
    fun hidden() = apply { styles.add("hidden") }

    fun black(bright: Boolean = false) = apply { styles.add(if (bright) "black bright" else "black") }
    fun red(bright: Boolean = false) = apply { styles.add(if (bright) "red bright" else "red") }
    fun green(bright: Boolean = false) = apply { styles.add(if (bright) "green bright" else "green") }
    fun yellow(bright: Boolean = false) = apply { styles.add(if (bright) "yellow bright" else "yellow") }
    fun blue(bright: Boolean = false) = apply { styles.add(if (bright) "blue bright" else "blue") }
    fun magenta(bright: Boolean = false) = apply { styles.add(if (bright) "magenta bright" else "magenta") }
    fun cyan(bright: Boolean = false) = apply { styles.add(if (bright) "cyan bright" else "cyan") }
    fun white(bright: Boolean = false) = apply { styles.add(if (bright) "white bright" else "white") }

    fun build() = styles.joinToString(",")
}