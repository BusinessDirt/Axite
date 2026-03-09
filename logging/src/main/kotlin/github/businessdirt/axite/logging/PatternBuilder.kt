package github.businessdirt.axite.logging

/**
 * A builder for Log4j2 patterns using a DSL syntax.
 * Allows for both colored and plain text patterns.
 */
class PatternBuilder {

    private val segments = mutableListOf<PatternSegment>()

    private interface PatternSegment {
        fun build(colored: Boolean): String
    }

    /**
     * Appends a literal string to the pattern.
     */
    fun text(text: String) = apply {
        segments.add(object : PatternSegment {
            override fun build(colored: Boolean) = text
        })
    }

    /**
     * Appends a timestamp to the pattern.
     */
    fun timestamp(format: String = "HH:mm:ss") = text("%d{$format}")

    /**
     * Appends the thread name to the pattern.
     */
    fun thread() = text("%t")

    /**
     * Appends the log level to the pattern.
     */
    fun level() = text("%level")

    /**
     * Appends the logger name to the pattern.
     */
    fun loggerName(precision: Int = 1) = text("%c{$precision}")

    /**
     * Appends the marker to the pattern, optionally with a style.
     */
    fun marker(style: String? = null) = apply {
        segments.add(object : PatternSegment {
            override fun build(colored: Boolean) = if (colored && style != null) {
                "%style{%marker}{$style}"
            } else {
                "%marker"
            }
        })
    }

    /**
     * Appends the marker to the pattern with a style.
     */
    fun marker(block: StyleBuilder.() -> Unit) = marker(StyleBuilder().apply(block).build())

    /**
     * Appends the message to the pattern, optionally with highlighting.
     */
    fun message(highlight: String? = null) = apply {
        segments.add(object : PatternSegment {
            override fun build(colored: Boolean) = if (colored && highlight != null) {
                "%highlight{%msg}{$highlight}"
            } else {
                "%msg"
            }
        })
    }

    /**
     * Appends the message to the pattern with highlighting.
     */
    fun message(block: HighlightBuilder.() -> Unit) = message(HighlightBuilder().apply(block).build())

    /**
     * Appends a line separator to the pattern.
     */
    fun line() = text("%n")

    fun wrap(prefix: String = "", suffix: String = "", block: PatternBuilder.() -> Unit) = apply {
        text(prefix)
        block()
        text(suffix)
    }

    /**
     * Wraps the content of the block in square brackets.
     */
    fun squareBrackets(prefix: String = "", suffix: String = "", block: PatternBuilder.() -> Unit) =
        wrap("$prefix[", "]$suffix", block)

    /**
     * Wraps the content of the block in brackets.
     */
    fun brackets(prefix: String = "", suffix: String = "", block: PatternBuilder.() -> Unit) =
        wrap("$prefix(", ")$suffix", block)

    /**
     * Appends a segment that is only included if its contents are not empty.
     */
    fun notEmpty(prefix: String = "", block: PatternBuilder.() -> Unit) = apply {
        val subBuilder = PatternBuilder()
        subBuilder.block()
        segments.add(object : PatternSegment {
            override fun build(colored: Boolean) = "%notEmpty{$prefix${subBuilder.build(colored)}}"
        })
    }

    /**
     * Builds the pattern string.
     */
    private fun build(colored: Boolean): String = segments.joinToString("") { it.build(colored) }

    /**
     * Returns the pattern with color formats.
     */
    fun withColors(): String = build(true)

    /**
     * Returns the pattern without color formats.
     */
    fun withoutColors(): String = build(false)

    companion object {
        /**
         * Creates an empty [PatternBuilder] and applies the [block].
         */
        fun empty(block: PatternBuilder.() -> Unit = {}) = PatternBuilder().apply(block)

        /**
         * Creates a simple [PatternBuilder].
         */
        fun simple() = empty {
            squareBrackets(suffix = " ") { timestamp() }
            squareBrackets(suffix = ": ") { level() }
            message()
            line()
        }

        /**
         * Creates a fancy [PatternBuilder] matching the default configuration.
         */
        fun fancy() = empty {
            squareBrackets(suffix = " ") { timestamp() }
            squareBrackets(suffix = " ") {
                thread()
                text("/")
                level()
            }
            brackets(suffix = ": ") {
                loggerName()
                notEmpty("/") {
                    marker { bold(); magenta() }
                }
            }
            message {
                fatal { red(bright = true) }
                error { red() }
                warn { yellow() }
                info { green() }
                debug { blue() }
                trace { white() }
            }
            line()
        }
    }
}
