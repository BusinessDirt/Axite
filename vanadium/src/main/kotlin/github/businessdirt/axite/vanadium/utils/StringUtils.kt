package github.businessdirt.axite.vanadium.utils

import org.slf4j.spi.LoggingEventBuilder

class BoxCharset(
    horizontal: Char,
    vertical: Char,
    topLeft: Char,
    topRight: Char,
    bottomLeft: Char,
    bottomRight: Char,
    topT: Char,
    bottomT: Char,
    leftT: Char,
    rightT: Char,
    cross: Char
) {
    private val chars = CharArray(16) { index ->
        when (index) {
            TOP -> '╵'
            BOTTOM -> '╷'
            LEFT -> '╴'
            RIGHT -> '╶'

            TOP or BOTTOM -> vertical
            LEFT or RIGHT -> horizontal
            BOTTOM or RIGHT -> topLeft
            BOTTOM or LEFT -> topRight
            TOP or RIGHT -> bottomLeft
            TOP or LEFT -> bottomRight

            BOTTOM or LEFT or RIGHT -> topT
            TOP or LEFT or RIGHT -> bottomT
            TOP or BOTTOM or RIGHT -> leftT
            TOP or BOTTOM or LEFT -> rightT

            TOP or BOTTOM or LEFT or RIGHT -> cross

            else -> ' ' // Index 0 (No connections) defaults to a space
        }
    }

    /**
     * Gets the character for a specific combination of connections.
     */
    operator fun get(top: Boolean, bottom: Boolean, left: Boolean, right: Boolean): Char {
        var index = 0
        if (top) index = index or TOP
        if (bottom) index = index or BOTTOM
        if (left) index = index or LEFT
        if (right) index = index or RIGHT
        return chars[index]
    }

    /**
     * Directly sets a character at a specific bitmask index.
     */
    fun setChar(index: Int, c: Char) {
        if (index in 0 until 16) chars[index] = c
    }

    companion object {
        private const val TOP = 1
        private const val BOTTOM = 2
        private const val LEFT = 4
        private const val RIGHT = 8

        /** Standard single-line box-drawing characters. */
        val SINGLE = BoxCharset('─', '│', '┌', '┐', '└', '┘', '┬', '┴', '├', '┤', '┼')

        /** Double-line box-drawing characters. */
        val DOUBLE = BoxCharset('═', '║', '╔', '╗', '╚', '╝', '╦', '╩', '╠', '╣', '╬').apply {
            setChar(TOP, '╹')
            setChar(BOTTOM, '╻')
            setChar(LEFT, '╸')
            setChar(RIGHT, '╺')
        }

        /** Single-line characters with rounded corners. */
        val ROUNDED = BoxCharset('─', '│', '╭', '╮', '╰', '╯', '┬', '┴', '├', '┤', '┼')

        /** Simple ASCII characters. */
        val ASCII = BoxCharset('-', '|', '+', '+', '+', '+', '+', '+', '+', '+', '+').apply {
            setChar(TOP, '^')
            setChar(BOTTOM, 'v')
            setChar(LEFT, '<')
            setChar(RIGHT, '>')
        }

        /** Heavy (bold) single-line characters. */
        val HEAVY = BoxCharset('━', '┃', '┏', '┓', '┗', '┛', '┳', '┻', '┣', '┫', '╋').apply {
            setChar(TOP, '╹')
            setChar(BOTTOM, '╻')
            setChar(LEFT, '╸')
            setChar(RIGHT, '╺')
        }
    }
}

fun String.boxed(
    boxCharset: BoxCharset = BoxCharset.ROUNDED,
    title: String? = null
): String {
    val content = this.trimEnd('\r', '\n')
    if (content.isBlank()) return this

    val lines = content.split("\\R".toRegex()).toTypedArray()
    val maxContentWidth = lines.maxOfOrNull { it.length } ?: 0

    // Format the title with spacing so it doesn't touch the lines
    val formattedTitle = title?.let { " $it " }
    val titleWidth = formattedTitle?.length ?: 0

    // The inner width must be wide enough for the content OR the title
    val innerWidth = maxOf(maxContentWidth, titleWidth)
    // Add 2 to account for the single space padding on the left and right of the content
    val totalBarWidth = innerWidth + 2

    val spacing = boxCharset[false, false, false, false]
    val vertical = boxCharset[true, true, false, false]
    val horizontal = boxCharset[false, false, true, true]

    val topLeft = boxCharset[false, true, false, true]
    val topRight = boxCharset[false, true, true, false]
    val bottomLeft = boxCharset[true, false, false, true]
    val bottomRight = boxCharset[true, false, true, false]

    val sb = StringBuilder()

    // --- TOP BORDER & TITLE ---
    sb.append(topLeft)
    if (formattedTitle != null) {
        val remainingBars = totalBarWidth - titleWidth
        sb.append(horizontal.toString().repeat(1))
        sb.append(formattedTitle)
        sb.append(horizontal.toString().repeat(remainingBars - 1))
    } else {
        sb.append(horizontal.toString().repeat(totalBarWidth))
    }
    sb.append(topRight).append("\n")

    // --- CONTENT ---
    for (line in lines) {
        sb.append(vertical).append(spacing).append(line)
        if (line.length < innerWidth) {
            // Pad the right side so the vertical wall aligns perfectly
            sb.append(spacing.toString().repeat(innerWidth - line.length))
        }
        sb.append(spacing).append(vertical).append("\n")
    }

    // --- BOTTOM BORDER ---
    sb.append(bottomLeft).append(horizontal.toString().repeat(totalBarWidth)).append(bottomRight)

    return sb.toString()
}

fun boxedString(
    boxCharset: BoxCharset = BoxCharset.ROUNDED,
    title: String? = null,
    builderAction: StringBuilder.() -> Unit
): String {
    val builder = StringBuilder()
    builder.builderAction()
    return builder.toString().boxed(boxCharset, title)
}

fun String.startsWith(vararg prefixes: String): Boolean =
    prefixes.any { this.startsWith(it) }

fun String.camelToTitleCase(): String = this.replace(Regex("([a-z])([A-Z]+)"), "$1 $2")
    .replaceFirstChar { it.uppercase() }

fun LoggingEventBuilder.log(
    boxCharset: BoxCharset = BoxCharset.ROUNDED,
    title: String? = null,
    builderAction: StringBuilder.() -> Unit
) = boxedString(boxCharset, title, builderAction).split("\\R".toRegex()).toTypedArray().forEach(::log)