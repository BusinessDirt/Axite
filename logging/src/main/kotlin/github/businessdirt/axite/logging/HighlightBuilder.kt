package github.businessdirt.axite.logging

/**
 * Builder for Log4j2 highlights.
 */
class HighlightBuilder {
    private val mappings = mutableListOf<String>()

    fun fatal(style: String) = apply { mappings.add("FATAL=$style") }
    fun fatal(block: StyleBuilder.() -> Unit) = fatal(StyleBuilder().apply(block).build())

    fun error(style: String) = apply { mappings.add("ERROR=$style") }
    fun error(block: StyleBuilder.() -> Unit) = error(StyleBuilder().apply(block).build())

    fun warn(style: String) = apply { mappings.add("WARN=$style") }
    fun warn(block: StyleBuilder.() -> Unit) = warn(StyleBuilder().apply(block).build())

    fun info(style: String) = apply { mappings.add("INFO=$style") }
    fun info(block: StyleBuilder.() -> Unit) = info(StyleBuilder().apply(block).build())

    fun debug(style: String) = apply { mappings.add("DEBUG=$style") }
    fun debug(block: StyleBuilder.() -> Unit) = debug(StyleBuilder().apply(block).build())

    fun trace(style: String) = apply { mappings.add("TRACE=$style") }
    fun trace(block: StyleBuilder.() -> Unit) = trace(StyleBuilder().apply(block).build())

    fun build() = mappings.joinToString(", ")
}